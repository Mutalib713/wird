package com.mosman.wird.data

import androidx.core.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object SafeFile {
    /**
     * Atomically writes string content to [file] using [AtomicFile].
     * Writes to a temporary file first and renames into place on success.
     * If an error occurs, the temporary write is aborted and the original file is preserved.
     */
    fun writeText(file: File, text: String) {
        val atomic = AtomicFile(file)
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(text.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (e: Exception) {
            if (stream != null) {
                atomic.failWrite(stream)
            }
            throw e
        }
    }

    /**
     * Atomically writes bytes to [file] using a `.part` temporary file and rename.
     * Prevents partial or half-written files from appearing in cache if interrupted.
     */
    fun writePart(file: File, bytes: ByteArray): Boolean {
        val part = File(file.parentFile, "${file.name}.part")
        return try {
            if (part.exists()) part.delete()
            part.writeBytes(bytes)
            if (file.exists()) file.delete()
            val renamed = part.renameTo(file)
            if (!renamed) {
                runCatching {
                    java.nio.file.Files.move(
                        part.toPath(),
                        file.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                }.isSuccess
            } else {
                true
            }
        } catch (e: Exception) {
            runCatching { part.delete() }
            false
        }
    }

    /**
     * Atomically writes text to [file] using a `.part` temporary file and rename.
     */
    fun writePart(file: File, text: String): Boolean =
        writePart(file, text.toByteArray(Charsets.UTF_8))

    /**
     * Renames an unreadable or corrupt file to a timestamped backup name
     * so that corrupted data is never overwritten and can be exported.
     */
    fun quarantineCorrupt(file: File, prefix: String = file.nameWithoutExtension): File {
        var timestamp = System.currentTimeMillis()
        var corruptFile = File(file.parentFile, "$prefix.corrupt-$timestamp.json")
        while (corruptFile.exists()) {
            timestamp++
            corruptFile = File(file.parentFile, "$prefix.corrupt-$timestamp.json")
        }
        file.renameTo(corruptFile)
        return corruptFile
    }
}
