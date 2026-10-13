package com.brendigo.folqio

import android.content.Context
import android.net.Uri
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * User-initiated export to a SAF destination. No broad-storage or network permissions.
 * Export only read-granted content:// PDFs, never arbitrary file:// paths.
 */
object PdfZipExport {
    const val MAX_FILES = 100
    const val MAX_BYTES = 300L * 1024 * 1024

    fun sanitizedName(value: String): String {
        val name = value.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^\\p{L}\\p{N}. _()-]"), "_")
            .trim().trim('.').take(100)
        val safe = name.ifBlank { "Document" }
        return if (safe.endsWith(".pdf", ignoreCase = true)) safe else "$safe.pdf"
    }

    fun export(context: Context, selected: List<LocalPdf>, destination: Uri): Int {
        require(selected.isNotEmpty() && selected.size <= MAX_FILES) { "Select 1–100 PDF documents." }
        require(destination.scheme == "content") { "Choose a location using the file picker." }

        val resolver = context.contentResolver
        val names = mutableSetOf<String>()
        var bytesTotal = 0L
        var completed = 0
        val output = resolver.openOutputStream(destination, "wt") ?: throw IOException("Cannot create ZIP archive.")
        output.use { raw ->
            ZipOutputStream(raw.buffered()).use { archive ->
                for (document in selected) {
                    val uri = Uri.parse(document.uri)
                    require(uri.scheme == "content") { "Only selected PDF sources can be exported." }
                    val base = sanitizedName(document.name)
                    var name = base
                    var suffix = 2
                    while (!names.add(name.lowercase(java.util.Locale.ROOT))) {
                        name = base.removeSuffix(".pdf").take(85) + " ($suffix).pdf"
                        suffix++
                    }
                    val input = resolver.openInputStream(uri)
                        ?: throw IOException("Cannot open ${document.name}.")
                    input.use {
                        archive.putNextEntry(ZipEntry(name))
                        val buffer = ByteArray(65536)
                        while (true) {
                            val read = it.read(buffer)
                            if (read < 0) break
                            bytesTotal += read
                            if (bytesTotal > MAX_BYTES) throw IOException("ZIP export exceeds 300 MB.")
                            archive.write(buffer, 0, read)
                        }
                        archive.closeEntry()
                    }
                    completed++
                }
            }
        }
        return completed
    }
}
