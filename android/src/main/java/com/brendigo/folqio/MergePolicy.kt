package com.brendigo.folqio

/**
 * Conservative bounds for raster PDF merging. Both inputs and page count are bounded
 * because PdfRenderer must rasterize every page on Android 8–14.
 */
object MergePolicy {
    const val MIN_FILES = 2
    const val MAX_FILES = 5
    const val MAX_PAGES = 50
    // Prevent oversized seekable SAF sources from exhausting PDF renderer resources.
    const val MAX_INPUT_BYTES = 150L * 1024 * 1024

    fun validFiles(count: Int): Boolean = count in MIN_FILES..MAX_FILES
    fun validPages(count: Int): Boolean = count in 1..MAX_PAGES
    // A SAF provider can report -1 when the file size is unknown.
    fun validReportedSize(bytes: Long): Boolean = bytes == -1L || bytes in 1..MAX_INPUT_BYTES
}
