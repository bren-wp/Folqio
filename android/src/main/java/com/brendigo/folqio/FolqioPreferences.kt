package com.brendigo.folqio

import android.content.Context

/** App-only user preferences. Never stores document bytes, account data or passwords. */
class FolqioPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("folqio_settings_v1", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("language", "en")?.takeIf { FolqioLanguages.supported.containsKey(it) } ?: "en"
        set(value) {
            require(FolqioLanguages.supported.containsKey(value)) { "Unsupported language" }
            prefs.edit().putString("language", value).apply()
        }

    var reopenLastDocument: Boolean
        get() = prefs.getBoolean("reopen_last_document", true)
        set(value) { prefs.edit().putBoolean("reopen_last_document", value).apply() }

    var confirmRemoval: Boolean
        get() = prefs.getBoolean("confirm_removal", true)
        set(value) { prefs.edit().putBoolean("confirm_removal", value).apply() }
}
