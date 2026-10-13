package com.brendigo.folqio

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Stream PDFs to a standards-compliant ZIP with bounded input and safe filenames.
 * No Android framework dependency: the whole archive can be tested on a JVM.
 */
internal object ZipArchiveWriter {
    const val MAX_FILES = 100
    const val MAX_BYTES = 300L * 1024 * 1024

    data class Source(val name: String, val open: () -> InputStream)

    fun sanitizedName(value: String): String {
        val leaf = value.replace('\\', '/').substringAfterLast('/')
            .replace(Regex("[^\\p{L}\\p{N}. _()-]"), "_")
            .trim().trim('.')
        val stem = (if (leaf.endsWith(".pdf", ignoreCase = true)) leaf.dropLast(4) else leaf)
            .trim().trim('.').take(96).ifBlank { "Document" }
        return "$stem.pdf"
    }

    fun write(sources: List<Source>, output: OutputStream, maxBytes: Long = MAX_BYTES): Int {
        require(sources.size in 1..MAX_FILES) { "Choose between 1 and 100 PDFs." }
        require(maxBytes in 1..MAX_BYTES) { "Invalid archive limit." }
        var count = 0
        var totalBytes = 0L
        val used = mutableSetOf<String>()
        ZipOutputStream(output.buffered(65536)).use { zip ->
            sources.forEach { source ->
                val base = sanitizedName(source.name)
                val stem = base.dropLast(4)
                var candidate = base
                var suffix = 2
                while (!used.add(candidate.lowercase(Locale.ROOT))) {
                    candidate = "${stem.take(80)} ($suffix).pdf"
                    suffix++
                }
                source.open().buffered(65536).use { input ->
                    zip.putNextEntry(ZipEntry(candidate))
                    val buffer = ByteArray(65536)
                    while (true) {
                        val size = input.read(buffer)
                        if (size == -1) break
                        totalBytes += size
                        if (totalBytes > maxBytes) {
                            throw IOException("Selected documents exceed the 300 MB export limit.")
                        }
                        zip.write(buffer, 0, size)
                    }
                    zip.closeEntry()
                }
                count++
            }
        }
        return count
    }
}
