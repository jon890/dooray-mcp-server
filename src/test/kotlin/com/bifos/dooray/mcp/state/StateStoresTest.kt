package com.bifos.dooray.mcp.state

import com.bifos.dooray.mcp.types.ResultId
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class StateStoresTest {
    @Test
    fun `메모리와 파일 멱등 저장소가 같은 예약 계약을 지킨다`() {
        val clock = MutableClock(1_000)
        val root = createTempDirectory("dooray-idempotency-test")
        try {
            val stores = listOf<IdempotencyStore>(
                InMemoryIdempotencyStore(clock),
                FileIdempotencyStore(root, clock),
            )
            stores.forEach { store ->
                assertIs<IdempotencyReservation.Reserved>(
                    store.reserve("post.create", "request-1", "fingerprint", "principal", 300_000)
                )
                assertIs<IdempotencyReservation.InProgress>(
                    store.reserve("post.create", "request-1", "fingerprint", "principal", 300_000)
                )
                assertIs<IdempotencyReservation.Conflict>(
                    store.reserve("post.create", "request-1", "different", "principal", 300_000)
                )
                store.complete(
                    "post.create",
                    "request-1",
                    "principal",
                    IdempotencyStatus.SUCCESS,
                    ids = listOf(ResultId("post", "1")),
                )
                assertIs<IdempotencyReservation.Replay>(
                    store.reserve("post.create", "request-1", "fingerprint", "principal", 300_000)
                )
                assertFailsWith<IllegalArgumentException> {
                    store.complete(
                        "post.create",
                        "request-1",
                        "principal",
                        IdempotencyStatus.OUTCOME_UNKNOWN,
                    )
                }
                clock.current += 300_001
                assertIs<IdempotencyReservation.Reserved>(
                    store.reserve("post.create", "request-1", "fingerprint", "principal", 300_000)
                )
                clock.current = 1_000
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `확인 토큰은 만료되고 한 번만 소비된다`() {
        val clock = MutableClock(10_000)
        val expectation = expectation()
        val store = InMemoryConfirmationStore("server-1", clock, tokenGenerator = { "token-1" })
        val grant = store.issue(expectation)
        assertEquals("token-1", grant.token)
        assertEquals(ConfirmationConsumeResult.Consumed, store.consume(grant.token, expectation))
        assertEquals(ConfirmationConsumeResult.AlreadyConsumed, store.consume(grant.token, expectation))

        val expiring = InMemoryConfirmationStore("server-1", clock, tokenGenerator = { "token-2" })
        expiring.issue(expectation, ttlMillis = 300_000)
        clock.current += 300_001
        assertEquals(ConfirmationConsumeResult.Expired, expiring.consume("token-2", expectation))
    }

    @Test
    fun `만료된 확인 기록을 메모리와 파일에서 정리한다`() {
        val root = createTempDirectory("dooray-confirmation-purge-test")
        try {
            val clock = MutableClock(1_000)
            val stores = listOf<ConfirmationStore>(
                InMemoryConfirmationStore("server-1", clock, tokenGenerator = { "memory-token" }),
                FileConfirmationStore(root, "server-1", clock, tokenGenerator = { "file-token" }),
            )
            val grants = stores.map { it.issue(expectation(), ttlMillis = 10) }
            clock.current += 11
            stores.forEach(ConfirmationStore::purgeExpired)
            stores.zip(grants).forEach { (store, grant) ->
                assertEquals(ConfirmationConsumeResult.NotFound, store.consume(grant.token, expectation()))
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `확인 토큰은 대상과 주체에 결합되고 파일에 원문을 저장하지 않는다`() {
        val root = createTempDirectory("dooray-confirmation-test")
        try {
            val clock = MutableClock(1_000)
            val store = FileConfirmationStore(
                root,
                "server-1",
                clock,
                tokenGenerator = { "plain-token-secret" },
            )
            val expectation = expectation()
            val grant = store.issue(expectation)
            val restartGrant = FileConfirmationStore(
                root,
                "server-1",
                clock,
                tokenGenerator = { "restart-token" },
            ).issue(expectation)
            val otherInstance = FileConfirmationStore(root, "server-2", clock, tokenGenerator = { "unused" })
            assertEquals(
                ConfirmationConsumeResult.NotFound,
                otherInstance.consume(restartGrant.token, expectation),
            )
            assertIs<ConfirmationConsumeResult.Mismatch>(
                store.consume(grant.token, expectation.copy(target = ResultId("comment", "different")))
            )
            assertEquals(ConfirmationConsumeResult.Consumed, store.consume(grant.token, expectation))

            val contents = Files.walk(root).use { paths ->
                paths.filter(Files::isRegularFile).map(Files::readString).toList().joinToString()
            }
            assertFalse(contents.contains("plain-token-secret"))
            assertTrue(contents.contains(StateFiles.hash("plain-token-secret")))

        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `손상된 상태 기록은 격리하고 사용하지 않는다`() {
        val root = createTempDirectory("dooray-corrupt-state-test")
        try {
            val record = root.resolve("idempotency/v1")
                .resolve(StateFiles.hash("principal\u0000post.create"))
                .resolve("${StateFiles.hash("request-1")}.json")
            Files.createDirectories(record.parent)
            Files.writeString(record, "{not-json")

            val store = FileIdempotencyStore(root, MutableClock(1_000))
            val error = assertFailsWith<IllegalStateException> {
                store.reserve("post.create", "request-1", "fingerprint", "principal", 300_000)
            }
            assertTrue(error.message!!.startsWith("STATE_CORRUPTED"))
            assertFalse(Files.exists(record))
            assertTrue(
                Files.list(root.resolve("quarantine")).use { it.findAny().isPresent },
                "손상된 기록이 quarantine으로 이동하지 않았습니다",
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `파일 저장소의 예약과 소비는 여러 인스턴스에서도 한 번만 성공한다`() {
        val root = createTempDirectory("dooray-state-concurrency-test")
        val executor = Executors.newFixedThreadPool(2)
        try {
            val clock = MutableClock(1_000)
            val firstIdempotency = FileIdempotencyStore(root, clock)
            val secondIdempotency = FileIdempotencyStore(root, clock)
            val reservations = listOf(firstIdempotency, secondIdempotency).map { store ->
                CompletableFuture.supplyAsync({
                    store.reserve("post.create", "request-1", "fingerprint", "principal", 300_000)
                }, executor)
            }.map(CompletableFuture<IdempotencyReservation>::join)
            assertEquals(1, reservations.count { it is IdempotencyReservation.Reserved })
            assertEquals(1, reservations.count { it is IdempotencyReservation.InProgress })

            val expectation = expectation()
            val issuer = FileConfirmationStore(root, "server-1", clock, tokenGenerator = { "token" })
            val grant = issuer.issue(expectation)
            val consumers = listOf(
                FileConfirmationStore(root, "server-1", clock),
                FileConfirmationStore(root, "server-1", clock),
            ).map { store ->
                CompletableFuture.supplyAsync({ store.consume(grant.token, expectation) }, executor)
            }.map(CompletableFuture<ConfirmationConsumeResult>::join)
            assertEquals(1, consumers.count { it == ConfirmationConsumeResult.Consumed })
            assertEquals(1, consumers.count { it == ConfirmationConsumeResult.AlreadyConsumed })
        } finally {
            executor.shutdownNow()
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `파일 잠금은 요청 수와 무관하게 제한된 스트라이프를 재사용한다`() {
        val root = createTempDirectory("dooray-state-lock-stripe-test")
        try {
            val paths = (0 until 10_000)
                .map { StateFiles.stripedLockPath(root, "idempotency", "key-$it") }
                .toSet()

            assertTrue(paths.size <= 4_096, "잠금 스트라이프 수가 제한을 넘었습니다: ${paths.size}")
            assertTrue(paths.size < 10_000, "요청마다 별도 잠금 경로가 만들어졌습니다")
            assertTrue(paths.all { it.fileName.toString().length == 6 })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `상태 디렉터리의 모든 하위 경로는 소유자 전용 권한을 사용한다`() {
        val root = createTempDirectory("dooray-state-permission-test")
        try {
            if (Files.getFileAttributeView(root, PosixFileAttributeView::class.java) == null) return
            Files.setPosixFilePermissions(root, PosixFilePermission.entries.toSet())

            FileIdempotencyStore(root, MutableClock(1_000)).reserve(
                "post.create",
                "request-1",
                "fingerprint",
                "principal",
                300_000,
            )

            val expected = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            )
            Files.walk(root).use { paths ->
                paths.filter(Files::isDirectory).forEach { directory ->
                    assertEquals(expected, Files.getPosixFilePermissions(directory), directory.toString())
                }
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun expectation() = ConfirmationExpectation(
        operation = "delete_comment",
        target = ResultId("comment", "1"),
        inputFingerprint = "input",
        targetSnapshotFingerprint = "snapshot",
        principalFingerprint = "principal",
    )

    private class MutableClock(var current: Long) : Clock {
        override fun nowEpochMillis(): Long = current
    }
}
