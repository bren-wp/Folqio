package com.brendigo.folqio

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

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
