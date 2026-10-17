package com.brendigo.folqio

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** Bound both image count and compressed bytes consumed from untrusted photo providers. */
object PhotoSelectionPolicy {
    const val MAX_PHOTOS = 10
    const val MAX_BYTES_PER_PHOTO = 25_000_000L

    fun isValidCount(count: Int): Boolean = count in 1..MAX_PHOTOS
}

/**
 * Prevent malicious or oversized image streams from consuming unbounded input.
 * Buffered decoders must wrap this stream, not the other way around, so mark/reset
 * cannot bypass the underlying byte budget.
 */
internal class BoundedPhotoInputStream(
    input: InputStream,
    private val maxBytes: Long = PhotoSelectionPolicy.MAX_BYTES_PER_PHOTO
) : FilterInputStream(input) {
    private var consumed = 0L

    init { require(maxBytes > 0) { "Invalid photo size limit." } }

    override fun read(): Int {
        val byte = super.read()
        if (byte != -1) {
            if (consumed >= maxBytes) throw IOException("Photo exceeds the 25 MB size limit.")
            consumed++
        }
        return byte
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || offset > buffer.size - length) {
            throw IndexOutOfBoundsException()
        }
        if (length == 0) return 0
        val remaining = maxBytes - consumed
        if (remaining == 0L) {
            if (super.read() != -1) throw IOException("Photo exceeds the 25 MB size limit.")
            return -1
        }
        val size = super.read(buffer, offset, minOf(remaining, length.toLong()).toInt())
        if (size > 0) consumed += size
        return size
    }
}
