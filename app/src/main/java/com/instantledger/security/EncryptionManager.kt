package com.instantledger.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteOpenHelper.Factory
import android.util.Log
import java.security.KeyStore
import javax.crypto.KeyGenerator
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

object EncryptionManager {
    private const val KEYSTORE_ALIAS = "InstantLedgerKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "instant_ledger_prefs"

    /**
     * Get SQLCipher passphrase from secure storage
     * @throws EncryptionException if passphrase generation or retrieval fails
     */
    fun getDatabasePassphrase(context: Context): String {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val passphrase = sharedPreferences.getString("db_passphrase", null)
            if (passphrase == null) {
                // Generate new passphrase
                val newPassphrase = generatePassphrase()
                sharedPreferences.edit()
                    .putString("db_passphrase", newPassphrase)
                    .apply()
                newPassphrase
            } else {
                passphrase
            }
        } catch (e: Exception) {
            Log.e("InstantLedger", "EncryptionManager: Failed to get database passphrase", e)
            throw EncryptionException(
                "Failed to generate or retrieve database encryption passphrase. " +
                "This is required for secure storage of financial data. " +
                "Error: ${e.message}",
                e
            )
        }
    }

    private fun generatePassphrase(): String {
        // Generate a secure random passphrase
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        return (1..64)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Create SQLCipher OpenHelperFactory using SupportFactory from SQLCipher.
     * @throws EncryptionException if SQLCipher cannot be initialized
     */
    fun createOpenHelperFactory(context: Context): Factory {
        val passphrase = getDatabasePassphrase(context)

        // Step 1: Load SQLCipher native libraries - CRITICAL: Must load before using
        try {
            SQLiteDatabase.loadLibs(context)
        } catch (e: Exception) {
            Log.e("InstantLedger", "EncryptionManager: Failed to load SQLCipher native libraries", e)
            throw EncryptionException(
                "Failed to load SQLCipher native libraries. " +
                "The database cannot be encrypted without SQLCipher. " +
                "Error: ${e.message}",
                e
            )
        }

        // Step 2: Create SQLCipher factory using SupportFactory
        // Use memory hygiene to clear passphrase bytes after use
        return MemoryHygiene.executeAndClearString(passphrase) { p ->
            val passphraseBytes = p?.toByteArray(Charsets.UTF_8)
            try {
                val factory = SupportFactory(passphraseBytes)
                Log.d("InstantLedger", "EncryptionManager: SQLCipher factory created successfully")
                Log.d("InstantLedger", "EncryptionManager: Database will be encrypted")
                factory as Factory
            } catch (e: Exception) {
                Log.e("InstantLedger", "EncryptionManager: Error creating SQLCipher factory", e)
                throw EncryptionException(
                    "Failed to create SQLCipher factory. " +
                    "This is required for database encryption. " +
                    "Error: ${e.message}",
                    e
                )
            } finally {
                MemoryHygiene.clearByteArray(passphraseBytes)
            }
        }
    }

    /**
     * Regenerate the encryption key stored in Android Keystore and rotate the
     * database passphrase. Key material remains inside Android Keystore; raw key
     * bytes are never exposed outside this class.
     */
    fun regenerateKey(context: Context) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            // Delete old key if exists
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            }

            // Generate a new key inside Android Keystore (GCM, no padding)
            generateKeystoreKey(keyStore)

            // Regenerate database passphrase
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val newPassphrase = generatePassphrase()
            sharedPreferences.edit()
                .putString("db_passphrase", newPassphrase)
                .apply()

            Log.d("EncryptionManager", "Encryption key regenerated successfully")
        } catch (e: Exception) {
            Log.e("EncryptionManager", "Error regenerating encryption key", e)
        }
    }

    /**
     * Generates a new AES key inside Android Keystore. The key material never leaves
     * the Keystore and is never returned as raw bytes.
     */
    private fun generateKeystoreKey(keyStore: KeyStore) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
}
