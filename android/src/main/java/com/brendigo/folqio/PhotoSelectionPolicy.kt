package com.brendigo.folqio

/** Limit peak PDF creation memory and keep a single import action predictable. */
object PhotoSelectionPolicy {
    const val MAX_PHOTOS = 10

    fun isValidCount(count: Int): Boolean = count in 1..MAX_PHOTOS
}
