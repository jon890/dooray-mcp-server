package com.bifos.dooray.mcp.state

import com.bifos.dooray.mcp.types.ResultId
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class IdempotencyStatus {
    IN_PROGRESS,
    SUCCESS,
    PARTIAL_SUCCESS,
    OUTCOME_UNKNOWN,
    FAILED_BEFORE_EFFECT,
}

@Serializable
data class IdempotencyRecord(
    val schemaVersion: Int = 1,
    val requestIdHash: String,
    val inputFingerprint: String,
    val principalFingerprint: String,
    val status: IdempotencyStatus,
    val resultFingerprint: String? = null,
    val ids: List<ResultId> = emptyList(),
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
)

sealed interface IdempotencyReservation {
    data class Reserved(val record: IdempotencyRecord) : IdempotencyReservation
    data class Replay(val record: IdempotencyRecord) : IdempotencyReservation
    data class InProgress(val record: IdempotencyRecord) : IdempotencyReservation
    data class Conflict(val record: IdempotencyRecord) : IdempotencyReservation
}

interface IdempotencyStore {
    fun reserve(
        scope: String,
        requestId: String,
        inputFingerprint: String,
        principalFingerprint: String,
        ttlMillis: Long,
    ): IdempotencyReservation

    fun complete(
        scope: String,
        requestId: String,
        principalFingerprint: String,
        status: IdempotencyStatus,
        resultFingerprint: String? = null,
        ids: List<ResultId> = emptyList(),
    ): IdempotencyRecord

    fun purgeExpired()
}

class InMemoryIdempotencyStore(
    private val clock: Clock = SystemClock,
) : IdempotencyStore {
    private val records = mutableMapOf<String, IdempotencyRecord>()

    @Synchronized
    override fun reserve(
        scope: String,
        requestId: String,
        inputFingerprint: String,
        principalFingerprint: String,
        ttlMillis: Long,
    ): IdempotencyReservation {
        require(ttlMillis > 0) { "ttlMillis must be positive" }
        val key = recordKey(scope, requestId, principalFingerprint)
        val now = clock.nowEpochMillis()
        val current = records[key]?.takeIf { it.expiresAtEpochMillis > now }
        if (current != null) {
            if (current.inputFingerprint != inputFingerprint) return IdempotencyReservation.Conflict(current)
            return if (current.status == IdempotencyStatus.IN_PROGRESS) {
                IdempotencyReservation.InProgress(current)
            } else {
                IdempotencyReservation.Replay(current)
            }
        }
        val record = newRecord(requestId, inputFingerprint, principalFingerprint, now, ttlMillis)
        records[key] = record
        return IdempotencyReservation.Reserved(record)
    }

    @Synchronized
    override fun complete(
        scope: String,
        requestId: String,
        principalFingerprint: String,
        status: IdempotencyStatus,
        resultFingerprint: String?,
        ids: List<ResultId>,
    ): IdempotencyRecord {
        require(status != IdempotencyStatus.IN_PROGRESS) { "completion status cannot be IN_PROGRESS" }
        val key = recordKey(scope, requestId, principalFingerprint)
        val current = records[key] ?: error("idempotency reservation not found")
        require(current.status == IdempotencyStatus.IN_PROGRESS) {
            "idempotency reservation is already completed"
        }
        return current.copy(status = status, resultFingerprint = resultFingerprint, ids = ids).also {
            records[key] = it
        }
    }

    @Synchronized
    override fun purgeExpired() {
        val now = clock.nowEpochMillis()
        records.entries.removeIf { it.value.expiresAtEpochMillis <= now }
    }
}

class FileIdempotencyStore(
    private val stateDirectory: Path,
    private val clock: Clock = SystemClock,
    private val json: Json = Json { ignoreUnknownKeys = false },
) : IdempotencyStore {
    @Synchronized
    override fun reserve(
        scope: String,
        requestId: String,
        inputFingerprint: String,
        principalFingerprint: String,
        ttlMillis: Long,
    ): IdempotencyReservation = StateFiles.withExclusiveLock(
        lockPath(scope, requestId, principalFingerprint),
        stateDirectory,
    ) {
        require(ttlMillis > 0) { "ttlMillis must be positive" }
        val path = path(scope, requestId, principalFingerprint)
        val now = clock.nowEpochMillis()
        val current = read(path)?.takeIf { it.expiresAtEpochMillis > now }
        if (current != null) {
            if (current.inputFingerprint != inputFingerprint) {
                return@withExclusiveLock IdempotencyReservation.Conflict(current)
            }
            return@withExclusiveLock if (current.status == IdempotencyStatus.IN_PROGRESS) {
                IdempotencyReservation.InProgress(current)
            } else {
                IdempotencyReservation.Replay(current)
            }
        }
        val record = newRecord(requestId, inputFingerprint, principalFingerprint, now, ttlMillis)
        write(path, record)
        IdempotencyReservation.Reserved(record)
    }

    @Synchronized
    override fun complete(
        scope: String,
        requestId: String,
        principalFingerprint: String,
        status: IdempotencyStatus,
        resultFingerprint: String?,
        ids: List<ResultId>,
    ): IdempotencyRecord = StateFiles.withExclusiveLock(
        lockPath(scope, requestId, principalFingerprint),
        stateDirectory,
    ) {
        require(status != IdempotencyStatus.IN_PROGRESS) { "completion status cannot be IN_PROGRESS" }
        val path = path(scope, requestId, principalFingerprint)
        val current = read(path) ?: error("idempotency reservation not found")
        require(current.status == IdempotencyStatus.IN_PROGRESS) {
            "idempotency reservation is already completed"
        }
        current.copy(status = status, resultFingerprint = resultFingerprint, ids = ids).also {
            write(path, it)
        }
    }

    @Synchronized
    override fun purgeExpired() {
        val root = stateDirectory.resolve("idempotency/v1")
        if (!java.nio.file.Files.exists(root)) return
        val now = clock.nowEpochMillis()
        java.nio.file.Files.walk(root).use { paths ->
            paths.filter { it.fileName.toString().endsWith(".json") }.forEach { file ->
                val scopeHash = file.parent.fileName.toString()
                val requestHash = file.fileName.toString().removeSuffix(".json")
                val lock = StateFiles.stripedLockPath(
                    stateDirectory,
                    "idempotency",
                    "$scopeHash\u0000$requestHash",
                )
                StateFiles.withExclusiveLock(lock, stateDirectory) {
                    val record = runCatching { read(file) }.getOrNull()
                    if (record?.expiresAtEpochMillis?.let { it <= now } == true) {
                        java.nio.file.Files.deleteIfExists(file)
                    }
                }
            }
        }
    }

    private fun path(scope: String, requestId: String, principal: String): Path =
        stateDirectory.resolve("idempotency/v1")
            .resolve(StateFiles.hash("$principal\u0000$scope"))
            .resolve("${StateFiles.hash(requestId)}.json")

    private fun lockPath(scope: String, requestId: String, principal: String): Path =
        StateFiles.stripedLockPath(
            stateDirectory,
            "idempotency",
            "${StateFiles.hash("$principal\u0000$scope")}\u0000${StateFiles.hash(requestId)}",
        )

    private fun read(path: Path): IdempotencyRecord? {
        val content = StateFiles.read(path) ?: return null
        return runCatching { json.decodeFromString<IdempotencyRecord>(content) }
            .getOrElse {
                StateFiles.quarantine(path, stateDirectory)
                throw IllegalStateException("STATE_CORRUPTED: idempotency record", it)
            }
    }

    private fun write(path: Path, record: IdempotencyRecord) =
        StateFiles.writeAtomically(path, json.encodeToString(record), stateDirectory)
}

private fun recordKey(scope: String, requestId: String, principal: String): String =
    "$principal\u0000$scope\u0000$requestId"

private fun newRecord(
    requestId: String,
    inputFingerprint: String,
    principalFingerprint: String,
    now: Long,
    ttlMillis: Long,
): IdempotencyRecord = IdempotencyRecord(
    requestIdHash = StateFiles.hash(requestId),
    inputFingerprint = inputFingerprint,
    principalFingerprint = principalFingerprint,
    status = IdempotencyStatus.IN_PROGRESS,
    createdAtEpochMillis = now,
    expiresAtEpochMillis = Math.addExact(now, ttlMillis),
)
