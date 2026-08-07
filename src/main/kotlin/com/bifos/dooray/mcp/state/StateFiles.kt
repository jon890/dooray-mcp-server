package com.bifos.dooray.mcp.state

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

internal object StateFiles {
    private val jvmLocks = ConcurrentHashMap<Path, Any>()
    private val ownerOnly = setOf(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
    )
    private val ownerDirectoryOnly = ownerOnly + PosixFilePermission.OWNER_EXECUTE

    fun hash(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        )

    fun read(path: Path): String? =
        if (Files.isRegularFile(path)) Files.readString(path) else null

    fun stripedLockPath(root: Path, namespace: String, key: String): Path =
        root.resolve("locks")
            .resolve(namespace)
            .resolve("${hash(key).take(LOCK_STRIPE_PREFIX_LENGTH)}.lck")

    fun writeAtomically(path: Path, content: String, privateRoot: Path) {
        val parent = path.parent
        ensurePrivateTree(privateRoot, parent)
        val temporary = Files.createTempFile(parent, ".dooray-mcp-", ".tmp")
        try {
            FileChannel.open(temporary, WRITE).use { channel ->
                val bytes = ByteBuffer.wrap(content.toByteArray(Charsets.UTF_8))
                while (bytes.hasRemaining()) channel.write(bytes)
                channel.force(true)
            }
            setPermissions(temporary, ownerOnly)
            Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING)
            setPermissions(path, ownerOnly)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun quarantine(path: Path, root: Path) {
        if (!Files.exists(path)) return
        val quarantine = root.resolve("quarantine")
        ensurePrivateTree(root, quarantine)
        Files.move(
            path,
            quarantine.resolve("${path.fileName}.${System.nanoTime()}.corrupt"),
            ATOMIC_MOVE,
        )
    }

    fun <T> withExclusiveLock(path: Path, privateRoot: Path, block: () -> T): T {
        val normalized = path.toAbsolutePath().normalize()
        val jvmLock = jvmLocks.computeIfAbsent(normalized) { Any() }
        return synchronized(jvmLock) {
            ensurePrivateTree(privateRoot, normalized.parent)
            FileChannel.open(normalized, CREATE, WRITE).use { channel ->
                setPermissions(normalized, ownerOnly)
                channel.lock().use { block() }
            }
        }
    }

    private fun ensurePrivateTree(root: Path, directory: Path) {
        val normalizedRoot = root.toAbsolutePath().normalize()
        val normalizedDirectory = directory.toAbsolutePath().normalize()
        require(normalizedDirectory.startsWith(normalizedRoot)) {
            "state directory escapes the configured root"
        }
        Files.createDirectories(normalizedDirectory)
        var current = normalizedRoot
        setPermissions(current, ownerDirectoryOnly)
        normalizedRoot.relativize(normalizedDirectory).forEach { segment ->
            current = current.resolve(segment)
            setPermissions(current, ownerDirectoryOnly)
        }
    }

    private fun setPermissions(path: Path, permissions: Set<PosixFilePermission>) {
        val view = Files.getFileAttributeView(
            path,
            java.nio.file.attribute.PosixFileAttributeView::class.java,
        ) ?: return
        view.setPermissions(permissions)
    }

    private const val LOCK_STRIPE_PREFIX_LENGTH = 2
}
