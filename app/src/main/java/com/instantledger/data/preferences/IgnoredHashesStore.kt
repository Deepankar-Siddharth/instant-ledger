package com.instantledger.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists SMS hashes that the user has chosen to ignore so the same
 * transaction is never shown again (e.g. duplicate SMS or repeat alerts).
 */
class IgnoredHashesStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun add(hash: String) {
        if (hash.isBlank()) return
        val current = prefs.getStringSet(KEY_HASHES, null)?.toMutableSet() ?: mutableSetOf()
        current.add(hash)
        prefs.edit().putStringSet(KEY_HASHES, current).apply()
    }

    fun contains(hash: String): Boolean {
        if (hash.isBlank()) return false
        return prefs.getStringSet(KEY_HASHES, null)?.contains(hash) ?: false
    }

    companion object {
        private const val PREFS_NAME = "instant_ledger_ignored_hashes"
        private const val KEY_HASHES = "ignored_sms_hashes"
    }
}
