package com.brendigo.folqio

import org.junit.Assert.*
import org.junit.Test

class LocalPdfTest {
    @Test fun acceptsCaseInsensitivePdfExtension() {
        assertTrue(LocalPdf.isPdfFilename("report.PDF"))
        assertTrue(LocalPdf.isPdfFilename("report.pdf"))
        assertFalse(LocalPdf.isPdfFilename("report.pdf.exe"))
    }

    @Test fun preservesDocumentIdentity() {
        val pdf = LocalPdf("content://provider/id/123", "Invoice.pdf")
        assertEquals("Invoice.pdf", pdf.name)
        assertEquals("content://provider/id/123", pdf.uri)
    }
}
