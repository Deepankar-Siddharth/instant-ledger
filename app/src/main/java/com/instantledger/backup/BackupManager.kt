package com.instantledger.backup

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.instantledger.data.database.AppDatabase
import net.sqlcipher.database.SQLiteDatabase
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages backup and restore operations for the Instant Ledger app.
 * Creates password-protected backup files containing database and preferences.
 */
class BackupManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BackupManager"
        private const val DB_NAME = "instant_ledger.db"
        private const val BACKUP_VERSION = 1
        private const val MANIFEST_FILE = "manifest.json"
        
        // SharedPreferences files to backup
        private const val PREFS_ENCRYPTED_SETTINGS = "instant_ledger_encrypted_settings"
        private const val PREFS_CATEGORIES = "instant_ledger_categories"
        private const val PREFS_PIN_LOCK = "instant_ledger_pin_lock"
        private const val PREFS_DB_PASSPHRASE = "instant_ledger_prefs"
    }
    
    /**
     * Creates a password-protected backup file containing database and all preferences.
     * @param password User-provided password for encryption
     * @param outputStream Output stream to write the backup file
     * @return true if backup was successful
     */
    suspend fun createBackup(password: String, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Generate encryption key from password
            val key = deriveKeyFromPassword(password)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16).apply { 
                java.security.SecureRandom().nextBytes(this)
            }
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            
            // Write IV first
            outputStream.write(iv)
            
            // Create encrypted ZIP output stream
            val zipOutputStream = ZipOutputStream(
                BufferedOutputStream(CipherOutputStream(outputStream, cipher))
            )
            
            try {
                // 1. Write manifest with backup metadata
                val manifest = createManifest()
                zipOutputStream.putNextEntry(ZipEntry(MANIFEST_FILE))
                zipOutputStream.write(manifest.toByteArray())
                zipOutputStream.closeEntry()
                
                // 2. Backup database file
                val dbFile = context.getDatabasePath(DB_NAME)
                if (dbFile.exists()) {
                    zipOutputStream.putNextEntry(ZipEntry(DB_NAME))
                    dbFile.inputStream().use { input ->
                        input.copyTo(zipOutputStream)
                    }
                    zipOutputStream.closeEntry()
                    Log.d(TAG, "Database backed up: ${dbFile.length()} bytes")
                } else {
                    Log.w(TAG, "Database file not found: ${dbFile.absolutePath}")
                }
                
                // 3. Backup all SharedPreferences
                // EncryptedSharedPreferences need special handling
                backupEncryptedSharedPreferences(PREFS_ENCRYPTED_SETTINGS, zipOutputStream)
                backupSharedPreferences(PREFS_CATEGORIES, zipOutputStream)
                backupEncryptedSharedPreferences(PREFS_PIN_LOCK, zipOutputStream)
                backupEncryptedSharedPreferences(PREFS_DB_PASSPHRASE, zipOutputStream)
                
                Log.d(TAG, "Backup completed successfully")
                Result.success(Unit)
            } finally {
                zipOutputStream.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restores data from a password-protected backup file.
     * @param password User-provided password for decryption
     * @param inputStream Input stream to read the backup file
     * @return true if restore was successful
     */
    suspend fun restoreBackup(password: String, inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Read IV
            val iv = ByteArray(16)
            inputStream.read(iv)
            
            // Generate decryption key from password
            val key = deriveKeyFromPassword(password)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            
            // Create decrypted ZIP input stream
            val zipInputStream = ZipInputStream(
                BufferedInputStream(CipherInputStream(inputStream, cipher))
            )
            
            try {
                var entry: ZipEntry?
                while (zipInputStream.nextEntry.also { entry = it } != null) {
                    entry?.let { zipEntry ->
                        when (zipEntry.name) {
                            MANIFEST_FILE -> {
                                // Verify manifest
                                val manifestBytes = zipInputStream.readBytes()
                                val manifest = String(manifestBytes)
                                verifyManifest(manifest)
                                Log.d(TAG, "Manifest verified: $manifest")
                            }
                            DB_NAME -> {
                                // Restore database
                                val dbFile = context.getDatabasePath(DB_NAME)
                                dbFile.parentFile?.mkdirs()
                                
                                // Close existing database connection
                                AppDatabase.getDatabase(context).close()
                                
                                // Write database file
                                dbFile.outputStream().use { output ->
                                    zipInputStream.copyTo(output)
                                }
                                Log.d(TAG, "Database restored: ${dbFile.length()} bytes")
                            }
                            else -> {
                                // Restore SharedPreferences (both regular and encrypted)
                                if (zipEntry.name.endsWith(".json")) {
                                    val prefsName = zipEntry.name.removeSuffix(".json")
                                    if (prefsName == PREFS_ENCRYPTED_SETTINGS || 
                                        prefsName == PREFS_PIN_LOCK || 
                                        prefsName == PREFS_DB_PASSPHRASE) {
                                        restoreEncryptedSharedPreferences(prefsName, zipInputStream)
                                    } else {
                                        restoreSharedPreferences(prefsName, zipInputStream)
                                    }
                                }
                            }
                        }
                        zipInputStream.closeEntry()
                    }
                }
                
                Log.d(TAG, "Restore completed successfully")
                Result.success(Unit)
            } finally {
                zipInputStream.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Derives a 256-bit AES key from a password using PBKDF2.
     */
    private fun deriveKeyFromPassword(password: String): ByteArray {
        val salt = "InstantLedgerBackup2024".toByteArray() // Fixed salt for consistency
        val keySpec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            10000, // Iterations
            256 // Key length in bits
        )
        val keyFactory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return keyFactory.generateSecret(keySpec).encoded
    }
    
    /**
     * Creates a manifest JSON file with backup metadata.
     */
    private fun createManifest(): String {
        return """
        {
            "version": $BACKUP_VERSION,
            "app_name": "Instant Ledger",
            "timestamp": ${System.currentTimeMillis()},
            "database_name": "$DB_NAME"
        }
        """.trimIndent()
    }
    
    /**
     * Verifies the manifest file format and version.
     */
    private fun verifyManifest(manifestJson: String) {
        // Basic validation - in production, parse JSON and check version compatibility
        if (!manifestJson.contains("\"version\":") || !manifestJson.contains("\"app_name\"")) {
            throw IllegalArgumentException("Invalid backup file format")
        }
    }
    
    /**
     * Backs up a SharedPreferences file to the ZIP archive.
     */
    private fun backupSharedPreferences(prefsName: String, zipOutputStream: ZipOutputStream) {
        try {
            val prefs = try {
                // Try regular SharedPreferences first
                context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            } catch (e: Exception) {
                Log.w(TAG, "Could not access $prefsName as regular SharedPreferences, trying encrypted", e)
                null
            }
            
            val allEntries = prefs?.all ?: emptyMap()
            
            if (allEntries.isEmpty()) {
                Log.d(TAG, "No entries in $prefsName, skipping")
                return
            }
            
            // Convert SharedPreferences to JSON
            val json = org.json.JSONObject()
            allEntries.forEach { (key, value) ->
                when (value) {
                    is String -> json.put(key, value)
                    is Int -> json.put(key, value)
                    is Long -> json.put(key, value)
                    is Float -> json.put(key, value.toDouble())
                    is Boolean -> json.put(key, value)
                    else -> json.put(key, value.toString())
                }
            }
            
            zipOutputStream.putNextEntry(ZipEntry("$prefsName.json"))
            zipOutputStream.write(json.toString().toByteArray())
            zipOutputStream.closeEntry()
            
            Log.d(TAG, "Backed up preferences: $prefsName (${allEntries.size} entries)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup preferences: $prefsName", e)
            // Continue with other preferences
        }
    }
    
    /**
     * Backs up EncryptedSharedPreferences by reading all keys and values.
     */
    private fun backupEncryptedSharedPreferences(prefsName: String, zipOutputStream: ZipOutputStream) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                prefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            val allEntries = encryptedPrefs.all
            
            if (allEntries.isEmpty()) {
                Log.d(TAG, "No entries in encrypted $prefsName, skipping")
                return
            }
            
            // Convert to JSON
            val json = org.json.JSONObject()
            allEntries.forEach { (key, value) ->
                when (value) {
                    is String -> json.put(key, value)
                    is Int -> json.put(key, value)
                    is Long -> json.put(key, value)
                    is Float -> json.put(key, value.toDouble())
                    is Boolean -> json.put(key, value)
                    else -> json.put(key, value.toString())
                }
            }
            
            zipOutputStream.putNextEntry(ZipEntry("$prefsName.json"))
            zipOutputStream.write(json.toString().toByteArray())
            zipOutputStream.closeEntry()
            
            Log.d(TAG, "Backed up encrypted preferences: $prefsName (${allEntries.size} entries)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup encrypted preferences: $prefsName", e)
            // Continue with other preferences
        }
    }
    
    /**
     * Restores a SharedPreferences file from the ZIP archive.
     */
    private fun restoreSharedPreferences(prefsName: String, zipInputStream: ZipInputStream) {
        try {
            val jsonBytes = zipInputStream.readBytes()
            val json = org.json.JSONObject(String(jsonBytes))
            
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.clear()
            
            json.keys().forEach { key ->
                val value = json.get(key)
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is Boolean -> editor.putBoolean(key, value)
                    else -> editor.putString(key, value.toString())
                }
            }
            
            editor.apply()
            Log.d(TAG, "Restored preferences: $prefsName (${json.length()} entries)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore preferences: $prefsName", e)
            throw e
        }
    }
    
    /**
     * Restores an EncryptedSharedPreferences file from the ZIP archive.
     */
    private fun restoreEncryptedSharedPreferences(prefsName: String, zipInputStream: ZipInputStream) {
        try {
            val jsonBytes = zipInputStream.readBytes()
            val json = org.json.JSONObject(String(jsonBytes))
            
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                prefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            val editor = encryptedPrefs.edit()
            editor.clear()
            
            json.keys().forEach { key ->
                val value = json.get(key)
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is Boolean -> editor.putBoolean(key, value)
                    else -> editor.putString(key, value.toString())
                }
            }
            
            editor.apply()
            Log.d(TAG, "Restored encrypted preferences: $prefsName (${json.length()} entries)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore encrypted preferences: $prefsName", e)
            throw e
        }
    }
    
    /**
     * Gets the size of the database file in bytes.
     */
    fun getDatabaseSize(): Long {
        val dbFile = context.getDatabasePath(DB_NAME)
        return if (dbFile.exists()) dbFile.length() else 0L
    }
}
