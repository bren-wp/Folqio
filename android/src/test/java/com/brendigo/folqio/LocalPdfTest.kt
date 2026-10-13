package com.brendigo.folqio

import org.junit.Assert.*
import org.junit.Test

class LocalPdfTest {
    @Test fun acceptsCaseInsensitivePdfExtension() {
        assertTrue(LocalPdf.isPdfFilename("report.PDF"))
        assertTrue(LocalPdf.isPdfFilename("report.pdf"))
        assertFalse(LocalPdf.isPdfFilename("report.pdf.exe"))
    }

    @Test fun generatedFilenamesAreStrictlySandboxed() {
        assertTrue(PdfLibrary.isGeneratedPdfFileName("55caa676-cdaa-4be1-aab2-39424168d200.pdf"))
        assertFalse(PdfLibrary.isGeneratedPdfFileName("../../secret.pdf"))
        assertFalse(PdfLibrary.isGeneratedPdfFileName("hello.pdf"))
        assertFalse(PdfLibrary.isGeneratedPdfFileName("55caa676-cdaa-4be1-aab2-39424168d200.pdf.exe"))
    }

    @Test fun preservesDocumentIdentity() {
        val pdf = LocalPdf("content://provider/id/123", "Invoice.pdf")
        assertEquals("Invoice.pdf", pdf.name)
        assertEquals("content://provider/id/123", pdf.uri)
    }
}
