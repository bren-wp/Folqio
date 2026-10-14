package com.brendigo.folqio

import org.junit.Assert.*
import org.junit.Test

class MergePolicyTest {
    @Test fun requiresTwoToFiveDocuments() {
        assertFalse(MergePolicy.validFiles(0))
        assertFalse(MergePolicy.validFiles(1))
        assertTrue(MergePolicy.validFiles(2))
        assertTrue(MergePolicy.validFiles(5))
        assertFalse(MergePolicy.validFiles(6))
    }

    @Test fun rejectsInvalidOrOversizedSeekableInputs() {
        assertTrue(MergePolicy.validReportedSize(-1)) // Unknown size from provider
        assertFalse(MergePolicy.validReportedSize(0))
        assertTrue(MergePolicy.validReportedSize(1024))
        assertTrue(MergePolicy.validReportedSize(MergePolicy.MAX_INPUT_BYTES))
        assertFalse(MergePolicy.validReportedSize(MergePolicy.MAX_INPUT_BYTES + 1))
    }

    @Test fun enforcesFinitePageCount() {
        assertFalse(MergePolicy.validPages(0))
        assertTrue(MergePolicy.validPages(1))
        assertTrue(MergePolicy.validPages(50))
        assertFalse(MergePolicy.validPages(51))
    }
}
