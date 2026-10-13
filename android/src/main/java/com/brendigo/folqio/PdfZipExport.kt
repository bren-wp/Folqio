package com.brendigo.folqio

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Build and validate the complete archive in app-private cache before writing to a
 * user-selected SAF destination. Source failures cannot leave a half-written export.
 */
object PdfZipExport {
    const val MAX_FILES = ZipArchiveWriter.MAX_FILES
    const val MAX_BYTES = ZipArchiveWriter.MAX_BYTES

    fun sanitizedName(value: String): String = ZipArchiveWriter.sanitizedName(value)

    fun export(context: Context, selected: List<LocalPdf>, destination: Uri): Int {
        require(selected.size in 1..MAX_FILES) { "Choose between 1 and 100 PDFs." }
        require(destination.scheme == "content") { "Choose a destination using the file picker." }
        val resolver = context.contentResolver
        val sources = selected.map { document ->
            val uri = Uri.parse(document.uri)
            require(uri.scheme == "content") { "Only PDF documents from the file picker can be exported." }
            ZipArchiveWriter.Source(document.name) {
                resolver.openInputStream(uri)
                    ?: throw IOException("Could not open ${document.name}.")
            }
        }
        val temporary = File.createTempFile("folqio-archive-", ".zip", context.cacheDir)
        try {
            val count = FileOutputStream(temporary).use { output ->
                ZipArchiveWriter.write(sources, output)
            }

            // Read entries through the ZIP decoder, which verifies each file's CRC.
            var checked = 0
            ZipInputStream(FileInputStream(temporary).buffered()).use { zip ->
                val buffer = ByteArray(65536)
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) throw IOException("Unexpected ZIP directory.")
                    while (zip.read(buffer) != -1) { /* verify stream */ }
                    zip.closeEntry()
                    checked++
                }
            }
            if (checked != count) throw IOException("ZIP verification failed.")

            val output = resolver.openOutputStream(destination, "wt")
                ?: throw IOException("Could not create the ZIP at the selected destination.")
            output.use { stream ->
                FileInputStream(temporary).use { it.copyTo(stream, 65536) }
            }
            return count
        } finally {
            temporary.delete()
        }
    }
}
