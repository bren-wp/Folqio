package com.brendigo.folqio

import org.junit.Assert.*
import org.junit.Test

class PdfZipExportTest {
    @Test fun sanitizesFileNamesAndExtensions() {
        assertEquals("report.pdf", PdfZipExport.sanitizedName("../report.pdf"))
        assertEquals("Report 2026.pdf", PdfZipExport.sanitizedName("Report 2026"))
        assertEquals("statement.pdf", PdfZipExport.sanitizedName("statement.pdf"))
        assertFalse(PdfZipExport.sanitizedName("../../private").contains("/"))
    }
}
