package com.instantledger.data.preferences

import android.content.Context
import android.content.SharedPreferences

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "instant_ledger_settings",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
        private const val KEY_PRIVACY_MODE = "privacy_mode_enabled"
    }
    
    fun isBiometricLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
    }
    
    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }
    
    fun isPrivacyModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_PRIVACY_MODE, false)
    }
    
    fun setPrivacyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
    }
    
}
