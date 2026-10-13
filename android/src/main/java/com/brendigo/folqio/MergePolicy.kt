package com.brendigo.folqio

/**
 * Conservative bounds for raster PDF merging. Both inputs and page count are bounded
 * because PdfRenderer must rasterize every page on Android 8–14.
 */
object MergePolicy {
    const val MIN_FILES = 2
    const val MAX_FILES = 5
    const val MAX_PAGES = 50

    fun validFiles(count: Int): Boolean = count in MIN_FILES..MAX_FILES
    fun validPages(count: Int): Boolean = count in 1..MAX_PAGES
}
