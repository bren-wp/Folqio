package com.brendigo.folqio

/** Guardrails for importing PDFs without retaining access to outside file providers. */
internal object PdfImportPolicy {
    const val MAX_BYTES = 150L * 1024 * 1024
    private val signature = "%PDF-".toByteArray(Charsets.US_ASCII)

    fun hasPdfSignature(header: ByteArray): Boolean {
        if (header.size < signature.size) return false
        for (start in 0..minOf(1024 - signature.size, header.size - signature.size)) {
            if (signature.indices.all { i -> header[start + i] == signature[i] }) return true
        }
        return false
    }

    fun documentName(raw: String?): String {
        val safe = raw.orEmpty()
            .replace('\\', '/').substringAfterLast('/')
            .filter { it != '\n' && it != '\r' && it != '\u0000' && !Character.isISOControl(it) }
            .trim().take(155).ifBlank { "Document" }
        return if (safe.endsWith(".pdf", ignoreCase = true)) safe else "$safe.pdf"
    }
}
