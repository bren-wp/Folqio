package com.brendigo.folqio

object PdfPageBounds {
    fun clamp(requested: Int, count: Int): Int {
        if (count <= 0) return 0
        return requested.coerceIn(0, count - 1)
    }
}
