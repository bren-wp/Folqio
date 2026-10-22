package com.brendigo.folqio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolqioLanguagesTest {
    @Test fun supportedLanguagesAndCoreLabelsStayAvailable() {
        assertEquals(25, FolqioLanguages.supported.size)
        for (language in FolqioLanguages.supported.keys) {
            assertTrue(FolqioLanguages.text(language, "home").isNotBlank())
            assertTrue(FolqioLanguages.text(language, "zipReady").isNotBlank())
        }
    }

    @Test fun zipCompletionUsesLocalizedCopy() {
        assertEquals("Arhiva je izrađena", FolqioLanguages.text("hr", "zipReady"))
        assertEquals("Archive created", FolqioLanguages.text("en", "zipReady"))
    }

    @Test fun unsupportedLanguageFallsBackToEnglish() {
        assertEquals("Archive created", FolqioLanguages.text("unknown", "zipReady"))
    }
}
