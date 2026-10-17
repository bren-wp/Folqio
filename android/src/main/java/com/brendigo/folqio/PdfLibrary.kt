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

    init {
        // An interrupted deletion must not make an otherwise intact PDF disappear.
        // Reconcile UUID-named tombstones using durable library metadata at startup.
        val owned = recent().mapNotNull { generatedFileName(it.uri) }.toSet()
        PendingPdfDeletion.recover(java.io.File(appContext.filesDir, "created"), owned)
    }

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
     * Transactional delete for an app-owned PDF. First rename to a sandbox-local
     * tombstone, then synchronously commit metadata. If the commit fails, restore
     * the original name. The next launch reconciles any interrupted deletion.
     * External provider documents are never deleted by Folqio.
     */
    fun remove(file: LocalPdf): Boolean {
        val documents = recent()
        if (documents.none { it.uri == file.uri }) return false

        val ownedName = generatedFileName(file.uri)
        val folder = java.io.File(appContext.filesDir, "created")
        if (ownedName != null && !PendingPdfDeletion.stage(folder, ownedName)) return false

        val favorites = preferences.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
        favorites.remove(file.uri)
        val bookmarks = preferences.getStringSet("bookmarks", emptySet()).orEmpty()
            .filterNot { it.startsWith("${file.uri}#") }.toSet()

        val remaining = JSONArray()
        documents.filterNot { it.uri == file.uri }.forEach { document ->
            remaining.put(JSONObject().put("uri", document.uri).put("name", document.name))
        }
        val committed = preferences.edit()
            .putString("recent", remaining.toString())
            .putStringSet("favorites", favorites)
            .putStringSet("bookmarks", bookmarks)
            .remove(lastPageKey(file.uri))
            .commit()

        if (!committed) {
            if (ownedName != null) PendingPdfDeletion.rollback(folder, ownedName)
            return false
        }
        // A failed cleanup is safe; startup recovery will remove the tombstone.
        if (ownedName != null) PendingPdfDeletion.finish(folder, ownedName)
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
            PendingPdfDeletion.isOwnedFileName(filename)
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
