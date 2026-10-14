package com.brendigo.folqio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import org.junit.Assert.*
import org.junit.Test

class ZipArchiveWriterTest {
    @Test fun exportsActualContentAndResolvesDuplicateNames() {
        val data = ByteArrayOutputStream()
        val count = ZipArchiveWriter.write(
            listOf(
                ZipArchiveWriter.Source("../Résumé.PDF") { ByteArrayInputStream("%PDF-a".toByteArray()) },
                ZipArchiveWriter.Source("Résumé.pdf") { ByteArrayInputStream("%PDF-b".toByteArray()) },
                ZipArchiveWriter.Source("folder\\third") { ByteArrayInputStream("%PDF-c".toByteArray()) }
            ), data
        )
        assertEquals(3, count)
        val names = mutableListOf<String>()
        val contents = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(data.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names.add(entry.name)
                contents.add(String(zip.readBytes()))
                zip.closeEntry()
            }
        }
        assertEquals(listOf("Résumé.pdf", "Résumé (2).pdf", "third.pdf"), names)
        assertEquals(listOf("%PDF-a", "%PDF-b", "%PDF-c"), contents)
    }

    @Test fun rejectsExcessDataInsteadOfSilentlyTruncating() {
        try {
            ZipArchiveWriter.write(
                listOf(ZipArchiveWriter.Source("too-big.pdf") {
                    ByteArrayInputStream(ByteArray(32))
                }),
                ByteArrayOutputStream(),
                maxBytes = 10
            )
            fail("Expected size limit")
        } catch (_: IOException) { }
    }

    @Test fun rejectsEmptySelectionAndUnsafeNames() {
        assertEquals("report.pdf", ZipArchiveWriter.sanitizedName("../../report.pdf"))
        assertEquals("Report 2026.pdf", ZipArchiveWriter.sanitizedName("Report 2026"))
        assertEquals("statement.pdf", ZipArchiveWriter.sanitizedName("statement.pdf"))
        assertEquals("Document.pdf", ZipArchiveWriter.sanitizedName("..."))
        assertFalse(ZipArchiveWriter.sanitizedName("../..\\private").contains("/"))
        try {
            ZipArchiveWriter.write(emptyList(), ByteArrayOutputStream())
            fail("Expected invalid selection")
        } catch (_: IllegalArgumentException) { }
    }
}
