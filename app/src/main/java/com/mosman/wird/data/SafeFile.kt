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
