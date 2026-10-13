package com.brendigo.folqio

import org.junit.Assert.*
import org.junit.Test

class PageMathTest {
    @Test fun pageBoundariesAreStable() {
        assertEquals(0, PdfPageBounds.clamp(-5, 12))
        assertEquals(11, PdfPageBounds.clamp(50, 12))
        assertEquals(4, PdfPageBounds.clamp(4, 12))
        assertEquals(0, PdfPageBounds.clamp(1, 0))
    }
}
