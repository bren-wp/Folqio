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
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("folqio_library_v1", Context.MODE_PRIVATE)

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

    /**
     * Never evict locally created documents merely because the recent list exceeded 30.
     * External provider shortcuts are limited, but created PDFs stay discoverable.
     */
    fun add(file: LocalPdf) {
        val all = listOf(file) + recent().filterNot { it.uri == file.uri }
        var externalCount = 0
        saveRecent(all.filter { item ->
            if (generatedFileName(item.uri) != null) true
            else ++externalCount <= 30
        })
    }

    /**
     * Removing an imported document only removes the shortcut; an app-created PDF
     * is permanently deleted after explicit confirmation in the UI.
     */
    fun remove(file: LocalPdf): Boolean {
        val ownedName = generatedFileName(file.uri)
        if (ownedName != null) {
            val ownedFile = java.io.File(appContext.filesDir, "created/$ownedName")
            if (ownedFile.exists() && !ownedFile.delete()) return false
        }

        saveRecent(recent().filterNot { it.uri == file.uri })
        val favorites = preferences.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
        favorites.remove(file.uri)
        val bookmarks = preferences.getStringSet("bookmarks", emptySet()).orEmpty()
            .filterNot { it.startsWith("${file.uri}#") }.toSet()
        preferences.edit()
            .putStringSet("favorites", favorites)
            .putStringSet("bookmarks", bookmarks)
            .remove(lastPageKey(file.uri))
            .apply()
        return true
    }

    private fun generatedFileName(uriString: String): String? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        if (uri.scheme != "content" || uri.authority != "${appContext.packageName}.files") {
            return null
        }
        if (uri.pathSegments.size != 2 || uri.pathSegments[0] != "created_pdfs") return null
        val name = uri.pathSegments[1]
        return name.takeIf { isGeneratedPdfFileName(it) }
    }

    companion object {
        fun isGeneratedPdfFileName(filename: String): Boolean =
            Regex(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.pdf"
            ).matches(filename)
    }

    private fun saveRecent(documents: List<LocalPdf>) {
        val items = JSONArray()
        documents.forEach { entry ->
            items.put(JSONObject().put("uri", entry.uri).put("name", entry.name))
        }
        preferences.edit().putString("recent", items.toString()).apply()
    }

    /**
     * Change the library display label; an imported provider-owned source is never renamed.
     */
    fun renameLabel(file: LocalPdf, requested: String): Boolean {
        val name = requested.trim().take(160)
        if (name.isBlank() || name.any { it == '/' || it == '\\' || it == '\n' || it == '\r' }) return false
        val normalized = if (LocalPdf.isPdfFilename(name)) name else "$name.pdf"
        val documents = recent()
        if (documents.none { it.uri == file.uri }) return false
        saveRecent(documents.map { if (it.uri == file.uri) it.copy(name = normalized) else it })
        return true
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
