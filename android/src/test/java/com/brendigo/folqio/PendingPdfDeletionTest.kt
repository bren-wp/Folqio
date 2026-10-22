package com.brendigo.folqio

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PendingPdfDeletionTest {
    @get:Rule val temporary = TemporaryFolder()

    private val owned = "55caa676-cdaa-4be1-aab2-39424168d200.pdf"
    private fun source(): File = File(temporary.root, owned)

    @Test fun interruptedDeletionRestoresTheOriginalBytes() {
        val contents = "%PDF-1.7\\nprivate test content".toByteArray()
        source().writeBytes(contents)
        assertTrue(PendingPdfDeletion.stage(temporary.root, owned))
        assertFalse(source().exists())

        PendingPdfDeletion.recover(temporary.root, setOf(owned))
        assertArrayEquals(contents, source().readBytes())
        assertFalse(File(temporary.root, owned.removeSuffix(".pdf") + ".pending-delete").exists())
    }

    @Test fun completedDeletionRemovesOnlyItsOwnTombstone() {
        source().writeText("private bytes")
        assertTrue(PendingPdfDeletion.stage(temporary.root, owned))
        val unrelated = File(temporary.root, "notes.pending-delete").apply { writeText("keep") }
        PendingPdfDeletion.recover(temporary.root, emptySet())

        assertFalse(source().exists())
        assertTrue(unrelated.exists())
        assertEquals("keep", unrelated.readText())
        assertFalse(File(temporary.root, owned.removeSuffix(".pdf") + ".pending-delete").exists())
    }

    @Test fun rollbackAfterMetadataFailureRestoresDocument() {
        source().writeText("unchanged PDF")
        assertTrue(PendingPdfDeletion.stage(temporary.root, owned))
        assertTrue(PendingPdfDeletion.rollback(temporary.root, owned))
        assertEquals("unchanged PDF", source().readText())
        assertFalse(PendingPdfDeletion.finish(temporary.root, owned))
    }

    @Test fun invalidAndMissingNamesCannotAffectFiles() {
        source().writeText("source")
        assertFalse(PendingPdfDeletion.stage(temporary.root, "../secret.pdf"))
        assertFalse(PendingPdfDeletion.stage(temporary.root, owned + ".bak"))
        assertFalse(PendingPdfDeletion.rollback(temporary.root, owned))
        assertEquals("source", source().readText())
    }

    @Test fun existingLivePdfIsNeverOverwrittenDuringRecovery() {
        val pending = File(temporary.root, owned.removeSuffix(".pdf") + ".pending-delete")
        pending.writeText("older tombstone")
        source().writeText("live source")
        PendingPdfDeletion.recover(temporary.root, setOf(owned))
        assertEquals("live source", source().readText())
        assertTrue(pending.exists())
        assertEquals("older tombstone", pending.readText())
    }

    @Test fun conflictingUnreferencedTombstoneIsAlsoPreserved() {
        val pending = File(temporary.root, owned.removeSuffix(".pdf") + ".pending-delete")
        pending.writeText("staged content")
        source().writeText("new live content")
        PendingPdfDeletion.recover(temporary.root, emptySet())
        assertEquals("new live content", source().readText())
        assertEquals("staged content", pending.readText())
    }
}
