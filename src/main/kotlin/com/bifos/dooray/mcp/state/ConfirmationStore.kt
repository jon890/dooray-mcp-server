package com.bifos.dooray.mcp.state

import com.bifos.dooray.mcp.types.ResultId
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ConfirmationRecord(
    val schemaVersion: Int = 1,
    val tokenHash: String,
    val operation: String,
    val target: ResultId,
    val inputFingerprint: String,
    val targetSnapshotFingerprint: String,
    val principalFingerprint: String,
    val serverInstanceId: String,
    val expiresAtEpochMillis: Long,
    val consumedAtEpochMillis: Long? = null,
)

data class ConfirmationGrant(
    val token: String,
    val expiresAtEpochMillis: Long,
)

sealed interface ConfirmationConsumeResult {
    data object Consumed : ConfirmationConsumeResult
    data object NotFound : ConfirmationConsumeResult
    data object Expired : ConfirmationConsumeResult
    data object AlreadyConsumed : ConfirmationConsumeResult
    data class Mismatch(val field: String) : ConfirmationConsumeResult
}

data class ConfirmationExpectation(
    val operation: String,
    val target: ResultId,
    val inputFingerprint: String,
    val targetSnapshotFingerprint: String,
    val principalFingerprint: String,
)

interface ConfirmationStore {
    fun issue(expectation: ConfirmationExpectation, ttlMillis: Long = 300_000): ConfirmationGrant
    fun consume(token: String, expectation: ConfirmationExpectation): ConfirmationConsumeResult
    fun purgeExpired()
}

class InMemoryConfirmationStore(
    private val serverInstanceId: String,
    private val clock: Clock = SystemClock,
    private val tokenGenerator: () -> String = ::secureToken,
) : ConfirmationStore {
    private val records = mutableMapOf<String, ConfirmationRecord>()

    @Synchronized
    override fun issue(expectation: ConfirmationExpectation, ttlMillis: Long): ConfirmationGrant {
        require(ttlMillis > 0) { "ttlMillis must be positive" }
        repeat(MAX_TOKEN_ATTEMPTS) {
            val token = tokenGenerator()
            val tokenHash = StateFiles.hash(token)
            if (tokenHash !in records) {
                val expiresAt = Math.addExact(clock.nowEpochMillis(), ttlMillis)
                records[tokenHash] = expectation.toRecord(
                    tokenHash = tokenHash,
                    serverInstanceId = serverInstanceId,
                    expiresAtEpochMillis = expiresAt,
                )
                return ConfirmationGrant(token, expiresAt)
            }
        }
        error("confirmation token generation failed")
    }

    @Synchronized
    override fun consume(token: String, expectation: ConfirmationExpectation): ConfirmationConsumeResult {
        val tokenHash = StateFiles.hash(token)
        val current = records[tokenHash] ?: return ConfirmationConsumeResult.NotFound
        val validation = validateConfirmation(current, expectation, serverInstanceId, clock.nowEpochMillis())
        if (validation != null) return validation
        records[tokenHash] = current.copy(consumedAtEpochMillis = clock.nowEpochMillis())
        return ConfirmationConsumeResult.Consumed
    }

    @Synchronized
    override fun purgeExpired() {
        val now = clock.nowEpochMillis()
        records.entries.removeIf { (_, record) -> shouldPurge(record, now) }
    }
}

class FileConfirmationStore(
    private val stateDirectory: Path,
    private val serverInstanceId: String,
    private val clock: Clock = SystemClock,
    private val tokenGenerator: () -> String = ::secureToken,
    private val json: Json = Json { ignoreUnknownKeys = false },
) : ConfirmationStore {
    @Synchronized
    override fun issue(expectation: ConfirmationExpectation, ttlMillis: Long): ConfirmationGrant {
        require(ttlMillis > 0) { "ttlMillis must be positive" }
        repeat(MAX_TOKEN_ATTEMPTS) {
            val token = tokenGenerator()
            val tokenHash = StateFiles.hash(token)
            val grant = StateFiles.withExclusiveLock(lockPath(tokenHash), stateDirectory) {
                if (StateFiles.read(path(tokenHash)) == null) {
                    val expiresAt = Math.addExact(clock.nowEpochMillis(), ttlMillis)
                    write(expectation.toRecord(tokenHash, serverInstanceId, expiresAt))
                    ConfirmationGrant(token, expiresAt)
                } else {
                    null
                }
            }
            if (grant != null) return grant
        }
        error("confirmation token generation failed")
    }

    @Synchronized
    override fun consume(token: String, expectation: ConfirmationExpectation): ConfirmationConsumeResult {
        val tokenHash = StateFiles.hash(token)
        return StateFiles.withExclusiveLock(lockPath(tokenHash), stateDirectory) {
            val current = read(tokenHash) ?: return@withExclusiveLock ConfirmationConsumeResult.NotFound
            val validation = validateConfirmation(current, expectation, serverInstanceId, clock.nowEpochMillis())
            if (validation != null) return@withExclusiveLock validation
            write(current.copy(consumedAtEpochMillis = clock.nowEpochMillis()))
            ConfirmationConsumeResult.Consumed
        }
    }

    @Synchronized
    override fun purgeExpired() {
        val root = stateDirectory.resolve("confirmations/v1")
        if (!java.nio.file.Files.exists(root)) return
        val now = clock.nowEpochMillis()
        java.nio.file.Files.list(root).use { paths ->
            paths.filter { it.fileName.toString().endsWith(".json") }.forEach { file ->
                val tokenHash = file.fileName.toString().removeSuffix(".json")
                StateFiles.withExclusiveLock(lockPath(tokenHash), stateDirectory) {
                    val record = runCatching { read(tokenHash) }.getOrNull()
                    if (record != null && shouldPurge(record, now)) {
                        java.nio.file.Files.deleteIfExists(file)
                    }
                }
            }
        }
    }

    private fun path(tokenHash: String): Path =
        stateDirectory.resolve("confirmations/v1/$tokenHash.json")

    private fun lockPath(tokenHash: String): Path =
        StateFiles.stripedLockPath(stateDirectory, "confirmation", tokenHash)

    private fun read(tokenHash: String): ConfirmationRecord? {
        val path = path(tokenHash)
        val content = StateFiles.read(path) ?: return null
        return runCatching { json.decodeFromString<ConfirmationRecord>(content) }
            .getOrElse {
                StateFiles.quarantine(path, stateDirectory)
                throw IllegalStateException("STATE_CORRUPTED: confirmation record", it)
            }
    }

    private fun write(record: ConfirmationRecord) =
        StateFiles.writeAtomically(path(record.tokenHash), json.encodeToString(record), stateDirectory)
}

private fun ConfirmationExpectation.toRecord(
    tokenHash: String,
    serverInstanceId: String,
    expiresAtEpochMillis: Long,
): ConfirmationRecord = ConfirmationRecord(
    tokenHash = tokenHash,
    operation = operation,
    target = target,
    inputFingerprint = inputFingerprint,
    targetSnapshotFingerprint = targetSnapshotFingerprint,
    principalFingerprint = principalFingerprint,
    serverInstanceId = serverInstanceId,
    expiresAtEpochMillis = expiresAtEpochMillis,
)

private fun validateConfirmation(
    record: ConfirmationRecord,
    expectation: ConfirmationExpectation,
    serverInstanceId: String,
    now: Long,
): ConfirmationConsumeResult? {
    if (record.serverInstanceId != serverInstanceId) return ConfirmationConsumeResult.NotFound
    if (record.consumedAtEpochMillis != null) return ConfirmationConsumeResult.AlreadyConsumed
    if (record.expiresAtEpochMillis <= now) return ConfirmationConsumeResult.Expired
    if (record.operation != expectation.operation) return ConfirmationConsumeResult.Mismatch("operation")
    if (record.target != expectation.target) return ConfirmationConsumeResult.Mismatch("target")
    if (record.inputFingerprint != expectation.inputFingerprint) {
        return ConfirmationConsumeResult.Mismatch("inputFingerprint")
    }
    if (record.targetSnapshotFingerprint != expectation.targetSnapshotFingerprint) {
        return ConfirmationConsumeResult.Mismatch("targetSnapshotFingerprint")
    }
    if (record.principalFingerprint != expectation.principalFingerprint) {
        return ConfirmationConsumeResult.Mismatch("principalFingerprint")
    }
    return null
}

private fun secureToken(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

private fun shouldPurge(record: ConfirmationRecord, now: Long): Boolean =
    record.expiresAtEpochMillis <= now ||
        record.consumedAtEpochMillis?.let { it + CONSUMED_RETENTION_MILLIS <= now } == true

private const val MAX_TOKEN_ATTEMPTS = 8
private const val CONSUMED_RETENTION_MILLIS = 3_600_000L
