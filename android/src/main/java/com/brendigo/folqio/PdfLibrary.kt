package com.brendigo.folqio

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class LocalPdf(val uri: String, val name: String) {
    companion object {
        fun isPdfFilename(name: String): Boolean = name.endsWith(".pdf", ignoreCase = true)
    }
}

class PdfLibrary(context: Context) {
    private val preferences = context.getSharedPreferences("folqio_library_v1", Context.MODE_PRIVATE)

    fun recent(): List<LocalPdf> {
        val entries = runCatching { JSONArray(preferences.getString("recent", "[]")) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until entries.length()) {
                val entry = entries.optJSONObject(i) ?: continue
                val uri = entry.optString("uri")
                val name = entry.optString("name")
                if (uri.startsWith("content://") && name.isNotBlank()) add(LocalPdf(uri, name))
            }
        }
    }

    fun add(file: LocalPdf) {
        val recent = (listOf(file) + recent().filterNot { it.uri == file.uri }).take(30)
        val entries = JSONArray()
        recent.forEach { entries.put(JSONObject().put("uri", it.uri).put("name", it.name)) }
        preferences.edit().putString("recent", entries.toString()).apply()
    }

    fun remove(file: LocalPdf) {
        val entries = JSONArray()
        recent().filterNot { it.uri == file.uri }.forEach {
            entries.put(JSONObject().put("uri", it.uri).put("name", it.name))
        }
        preferences.edit().putString("recent", entries.toString()).apply()
    }

    /**
     * Change the library display label; an imported provider-owned source is never renamed.
     */
    fun renameLabel(file: LocalPdf, requested: String): Boolean {
        val name = requested.trim().take(160)
        if (name.isBlank() || name.any { it == '/' || it == '\\' || it == '\n' || it == '\r' }) return false
        val normalized = if (LocalPdf.isPdfFilename(name)) name else "$name.pdf"
        val entries = JSONArray()
        var found = false
        recent().forEach { item ->
            if (item.uri == file.uri) {
                entries.put(JSONObject().put("uri", item.uri).put("name", normalized))
                found = true
            } else {
                entries.put(JSONObject().put("uri", item.uri).put("name", item.name))
            }
        }
        if (found) preferences.edit().putString("recent", entries.toString()).apply()
        return found
    }

    private fun lastPageKey(uri: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(uri.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "last_page_$hash"
    }

    fun lastPage(uri: String): Int = preferences.getInt(lastPageKey(uri), 0).coerceAtLeast(0)

    fun recordPage(uri: String, page: Int) {
        preferences.edit().putInt(lastPageKey(uri), page.coerceAtLeast(0)).apply()
    }

    fun isFavorite(uri: String): Boolean =
        preferences.getStringSet("favorites", emptySet())?.contains(uri) == true

    fun toggleFavorite(uri: String): Boolean {
        val favorites = preferences.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
        if (!favorites.add(uri)) favorites.remove(uri)
        preferences.edit().putStringSet("favorites", favorites).apply()
        return uri in favorites
    }

    fun isBookmarked(uri: String, page: Int): Boolean =
        preferences.getStringSet("bookmarks", emptySet())?.contains("$uri#$page") == true

    fun toggleBookmark(uri: String, page: Int): Boolean {
        val bookmarks = preferences.getStringSet("bookmarks", emptySet()).orEmpty().toMutableSet()
        val key = "$uri#$page"
        if (!bookmarks.add(key)) bookmarks.remove(key)
        preferences.edit().putStringSet("bookmarks", bookmarks).apply()
        return key in bookmarks
    }
}
