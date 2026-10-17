package com.brendigo.folqio

import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Test

class PhotoSelectionPolicyTest {
    @Test fun requiresAtLeastOnePhoto() {
        assertFalse(PhotoSelectionPolicy.isValidCount(0))
        assertTrue(PhotoSelectionPolicy.isValidCount(1))
    }

    @Test fun permitsExactlyTheBudgetAndReachesEof() {
        BoundedPhotoInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3).use { stream ->
            assertArrayEquals(byteArrayOf(1, 2, 3), stream.readBytes())
        }
    }

    @Test fun rejectsExtraByteAfterSingleAndBufferedReads() {
        BoundedPhotoInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 3).use { stream ->
            assertEquals(1, stream.read())
            val rest = ByteArray(2)
            assertEquals(2, stream.read(rest, 0, rest.size))
            assertArrayEquals(byteArrayOf(2, 3), rest)
            assertThrows(IOException::class.java) { stream.read() }
        }
        BoundedPhotoInputStream(ByteArrayInputStream(byteArrayOf(1, 2)), 1).use { stream ->
            assertThrows(IOException::class.java) { stream.readBytes() }
        }
    }

    @Test fun capsImportSize() {
        assertTrue(PhotoSelectionPolicy.isValidCount(10))
        assertFalse(PhotoSelectionPolicy.isValidCount(11))
        assertFalse(PhotoSelectionPolicy.isValidCount(-1))
        assertEquals(25_000_000L, PhotoSelectionPolicy.MAX_BYTES_PER_PHOTO)
    }
}
