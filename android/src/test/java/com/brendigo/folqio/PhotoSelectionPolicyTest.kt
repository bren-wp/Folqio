package com.brendigo.folqio

import org.junit.Assert.*
import org.junit.Test

class PhotoSelectionPolicyTest {
    @Test fun requiresAtLeastOnePhoto() {
        assertFalse(PhotoSelectionPolicy.isValidCount(0))
        assertTrue(PhotoSelectionPolicy.isValidCount(1))
    }

    @Test fun capsImportSize() {
        assertTrue(PhotoSelectionPolicy.isValidCount(10))
        assertFalse(PhotoSelectionPolicy.isValidCount(11))
        assertFalse(PhotoSelectionPolicy.isValidCount(-1))
    }
}
