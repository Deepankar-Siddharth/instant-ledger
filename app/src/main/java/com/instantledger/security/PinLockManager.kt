package com.instantledger.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "instant_ledger_pin_lock",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_ENABLED = "pin_enabled"
    }

    fun isPinSet(): Boolean {
        return prefs.getBoolean(KEY_PIN_ENABLED, false) && prefs.contains(KEY_PIN_HASH)
    }

    fun setPin(pin: String): Boolean {
        return if (pin.length in 4..6 && pin.all { it.isDigit() }) {
            val hash = hashPin(pin)
            prefs.edit()
                .putString(KEY_PIN_HASH, hash)
                .putBoolean(KEY_PIN_ENABLED, true)
                .apply()
            true
        } else {
            false
        }
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest((pin + "instant_ledger_salt").toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
