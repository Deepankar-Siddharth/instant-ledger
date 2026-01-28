package com.instantledger.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted SharedPreferences for sensitive settings
 * All preferences are encrypted at rest using Android Keystore
 */
@Singleton
class EncryptedSettingsPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "instant_ledger_encrypted_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
        private const val KEY_PRIVACY_MODE = "privacy_mode_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_NEW_TRANSACTION_NOTIFICATIONS = "new_transaction_notifications_enabled"
        private const val KEY_REMINDER_NOTIFICATIONS = "reminder_notifications_enabled"
    }
    
    fun isBiometricLockEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
    }
    
    fun setBiometricLockEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }
    
    fun isPrivacyModeEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_PRIVACY_MODE, false)
    }
    
    fun setPrivacyModeEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
    }
    
    fun setPinHash(hash: String, salt: String) {
        encryptedPrefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
    }
    
    fun getPinHash(): Pair<String?, String?> {
        return Pair(
            encryptedPrefs.getString(KEY_PIN_HASH, null),
            encryptedPrefs.getString(KEY_PIN_SALT, null)
        )
    }
    
    fun hasPin(): Boolean {
        return encryptedPrefs.getString(KEY_PIN_HASH, null) != null
    }
    
    fun areNewTransactionNotificationsEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_NEW_TRANSACTION_NOTIFICATIONS, true) // Default enabled
    }
    
    fun setNewTransactionNotificationsEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_NEW_TRANSACTION_NOTIFICATIONS, enabled).apply()
    }
    
    fun areReminderNotificationsEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_REMINDER_NOTIFICATIONS, true) // Default enabled
    }
    
    fun setReminderNotificationsEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_REMINDER_NOTIFICATIONS, enabled).apply()
    }
    
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}
