package com.brendigo.folqio

import org.junit.Assert.*
import org.junit.Test

class PdfImportPolicyTest {
    @Test fun acceptsHeaderAtStartOrWithinPdfPrefix() {
        assertTrue(PdfImportPolicy.hasPdfSignature("%PDF-1.7\n".toByteArray()))
        assertTrue(PdfImportPolicy.hasPdfSignature(ByteArray(20) + "%PDF-2.0".toByteArray()))
        assertFalse(PdfImportPolicy.hasPdfSignature("This is not a PDF".toByteArray()))
        assertFalse(PdfImportPolicy.hasPdfSignature(byteArrayOf(37, 80, 68)))
        assertFalse(PdfImportPolicy.hasPdfSignature(ByteArray(1025) + "%PDF-".toByteArray()))
    }

    @Test fun cleansUntrustedProviderNames() {
        assertEquals("report.pdf", PdfImportPolicy.documentName("../../report.pdf"))
        assertEquals("Statement.PDF", PdfImportPolicy.documentName("Statement.PDF"))
        assertEquals("Document.pdf", PdfImportPolicy.documentName(""))
        assertEquals("badsafe.pdf", PdfImportPolicy.documentName("bad\r\nsafe"))
        assertFalse(PdfImportPolicy.documentName("bad\\name").contains("\\"))
    }

    @Test fun enforcesFiniteLimit() {
        assertEquals(150L * 1024 * 1024, PdfImportPolicy.MAX_BYTES)
    }
}
