package com.brendigo.folqio

import java.io.File

/**
 * Crash-recoverable deletion of UUID-named app-owned PDFs.
 *
 * A tombstone is always inside the same private directory as the original, so
 * staging is a rename rather than a copy. No external content URI is touched.
 */
internal object PendingPdfDeletion {
    private val filenamePattern = Regex(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.pdf"
    )

    fun isOwnedFileName(filename: String): Boolean = filenamePattern.matches(filename)

    private fun pendingFile(folder: File, filename: String): File =
        File(folder, filename.removeSuffix(".pdf") + ".pending-delete")

    fun stage(folder: File, filename: String): Boolean {
        if (!isOwnedFileName(filename) || !folder.isDirectory) return false
        val original = File(folder, filename)
        val pending = pendingFile(folder, filename)
        return original.isFile && !pending.exists() && original.renameTo(pending)
    }

    fun rollback(folder: File, filename: String): Boolean {
        if (!isOwnedFileName(filename)) return false
        val original = File(folder, filename)
        val pending = pendingFile(folder, filename)
        return !original.exists() && pending.isFile && pending.renameTo(original)
    }

    fun finish(folder: File, filename: String): Boolean {
        if (!isOwnedFileName(filename)) return false
        val pending = pendingFile(folder, filename)
        return pending.isFile && pending.delete()
    }

    /**
     * If metadata still lists the PDF, an interrupted delete must be rolled back.
     * Otherwise only strict UUID tombstones are removed. Never overwrite a live
     * PDF in the event both a live file and a leftover tombstone are present.
     */
    fun recover(folder: File, referencedNames: Set<String>) {
        folder.listFiles()?.forEach { pending ->
            if (!pending.isFile || !pending.name.endsWith(".pending-delete")) return@forEach
            val filename = pending.name.removeSuffix(".pending-delete") + ".pdf"
            if (!isOwnedFileName(filename)) return@forEach
            val original = File(folder, filename)
            if (original.exists()) {
                // A conflicting live file may contain different bytes. Do not
                // silently destroy the staged copy: preserve both for recovery.
                return@forEach
            }
            if (filename in referencedNames) {
                pending.renameTo(original)
            } else {
                pending.delete()
            }
        }
    }
}
