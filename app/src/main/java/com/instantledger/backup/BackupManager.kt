package com.instantledger.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.instantledger.data.database.AppDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Manages backup and restore operations for the Instant Ledger app.
 *
 * Backup format version 2 uses AES-256-GCM authenticated encryption with a random
 * per-backup salt, a random per-operation nonce, and PBKDF2-HMAC-SHA256 (600,000
 * iterations). Format version 1 (legacy: AES/CBC, fixed salt, 10,000 iterations)
 * remains restorable for backward compatibility.
 *
 * Restore is atomic: the entire backup is parsed and validated in memory before any
 * change is applied; on any failure the previous database and preferences are left
 * unchanged.
 */
class BackupManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupManager"
        private const val DB_NAME = "instant_ledger.db"
        private const val BACKUP_VERSION = 2
        private const val MANIFEST_FILE = "manifest.json"

        // Format identifiers
        private val MAGIC = "ILB2".toByteArray(Charsets.UTF_8) // 4 bytes
        private const val ALGORITHM_ID = 1 // AES-256-GCM
        private const val KDF_ID = 1 // PBKDF2-HMAC-SHA256
        private const val PBKDF2_ITERATIONS = 600_000
        private const val SALT_LENGTH = 16
        private const val NONCE_LENGTH = 12
        private const val TAG_LENGTH = 16

        // Legacy format version 1 identifiers
        private const val LEGACY_PBKDF2_ITERATIONS = 10_000
        private val LEGACY_SALT = "InstantLedgerBackup2024".toByteArray(Charsets.UTF_8)
        private const val LEGACY_IV_LENGTH = 16

        // SharedPreferences files to backup
        private const val PREFS_ENCRYPTED_SETTINGS = "instant_ledger_encrypted_settings"
        private const val PREFS_CATEGORIES = "instant_ledger_categories"
        private const val PREFS_PIN_LOCK = "instant_ledger_pin_lock"
        private const val PREFS_DB_PASSPHRASE = "instant_ledger_prefs"

        private val PREFS_NAMES = listOf(
            PREFS_ENCRYPTED_SETTINGS to true,
            PREFS_CATEGORIES to false,
            PREFS_PIN_LOCK to true,
            PREFS_DB_PASSPHRASE to true
        )
    }

    /**
     * Parsed header describing either a version-2 backup or a legacy version-1 backup.
     */
    private data class ParsedHeader(
        val isLegacy: Boolean,
        val salt: ByteArray,
        val iterations: Int,
        val nonce: ByteArray, // IV for legacy, nonce for v2
        val tag: ByteArray?, // null for legacy
        val ciphertextOffset: Int // offset into the raw file where encrypted ZIP begins
    )

    /**
     * Parsed backup contents held in memory before atomic application.
     */
    private data class BackupEntries(
        val manifest: String,
        val databaseBytes: ByteArray,
        val preferences: Map<String, Map<String, Any>>
    )

    /**
     * Creates a password-protected backup file containing database and all preferences.
     * Format: header (unencrypted metadata) || AES-256-GCM-encrypted ZIP archive.
     */
    suspend fun createBackup(password: String, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        var key: ByteArray? = null
        try {
            // 1. Build the ZIP archive in memory (manifest + database + preferences)
            val zipBytes = buildZipArchive()

            // 2. Generate random salt and nonce
            val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
            val nonce = ByteArray(NONCE_LENGTH).apply { SecureRandom().nextBytes(this) }

            // 3. Derive key from password (PBKDF2-HMAC-SHA256, 600k iterations)
            key = deriveKeyFromPassword(password, salt, PBKDF2_ITERATIONS)

            // 4. Encrypt ZIP with AES-256-GCM (authenticated; tag is verified on restore)
            val (ciphertext, tag) = encryptGcm(zipBytes, key, nonce)

            // 5. Write header then ciphertext
            val header = buildHeader(salt, nonce, tag)
            outputStream.write(header)
            outputStream.write(ciphertext)
            outputStream.flush()

            logI("Backup completed successfully (format v2, AES-256-GCM)")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Backup failed", e)
            Result.failure(e)
        } finally {
            key?.let { clearByteArray(it) }
        }
    }

    /**
     * Restores data from a password-protected backup file.
     * Detects format version automatically (v2 or legacy v1), verifies authentication
     * (v2), then applies the backup atomically with rollback on any failure.
     */
    suspend fun restoreBackup(password: String, inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Read the entire backup into memory
            val allBytes = inputStream.readFully()

            // 2. Parse header (detects v2 vs legacy v1), derive key
            val parsed = parseHeader(allBytes)
            var key: ByteArray? = null
            try {
                key = deriveKeyFromPassword(password, parsed.salt, parsed.iterations)

                // 3. Decrypt the ZIP payload (v2: verifies GCM auth tag; legacy: CBC)
                val zipBytes = if (parsed.isLegacy) {
                    val ciphertext = allBytes.copyOfRange(parsed.ciphertextOffset, allBytes.size)
                    decryptCbc(ciphertext, key, parsed.nonce)
                } else {
                    val ciphertext = allBytes.copyOfRange(parsed.ciphertextOffset, allBytes.size)
                    decryptGcm(ciphertext, parsed.tag ?: ByteArray(0), key, parsed.nonce)
                }

                // 4. Parse ZIP entries into memory
                val entries = parseZip(zipBytes)

                // 5. Validate manifest before touching any state
                validateManifest(entries.manifest)

                // 6. Apply atomically (rollback DB + prefs on any failure)
                applyAtomically(context, entries)

                logI("Restore completed successfully (format v${if (parsed.isLegacy) 1 else 2})")
                Result.success(Unit)
            } finally {
                key?.let { clearByteArray(it) }
            }
        } catch (e: Exception) {
            logE("Restore failed - existing records left unchanged", e)
            Result.failure(e)
        }
    }

    /**
     * Builds the ZIP archive (manifest + database + preferences) held in memory.
     */
    private fun buildZipArchive(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            // Manifest
            val manifest = createManifest()
            zip.putNextEntry(ZipEntry(MANIFEST_FILE))
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // Database file
            val dbFile = context.getDatabasePath(DB_NAME)
            if (dbFile.exists()) {
                zip.putNextEntry(ZipEntry(DB_NAME))
                FileInputStream(dbFile).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            } else {
                logW("Database file not found during backup: ${dbFile.absolutePath}")
            }

            // Preferences (JSON)
            PREFS_NAMES.forEach { (prefsName, encrypted) ->
                backupPrefsToZip(zip, prefsName, encrypted)
            }
        }
        return baos.toByteArray()
    }

    /**
     * Writes one SharedPreferences file as a JSON ZIP entry.
     */
    private fun backupPrefsToZip(zip: ZipOutputStream, prefsName: String, encrypted: Boolean) {
        try {
            val allEntries = if (encrypted) {
                readEncryptedPrefs(prefsName)
            } else {
                readRegularPrefs(prefsName)
            }
            if (allEntries.isEmpty()) {
                logD("No entries in $prefsName, skipping")
                return
            }
            val json = JSONObject()
            allEntries.forEach { (k, v) -> json.put(k, v) }
            zip.putNextEntry(ZipEntry("$prefsName.json"))
            zip.write(json.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            logD("Backed up preferences: $prefsName (${allEntries.size} entries)")
        } catch (e: Exception) {
            logW("Failed to backup preferences: $prefsName", e)
            // Continue with other preferences; do not abort the whole backup
        }
    }

    /**
     * Reads all entries from a regular SharedPreferences file.
     */
    private fun readRegularPrefs(prefsName: String): Map<String, Any> {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.all.toMap()
    }

    /**
     * Reads all entries from an EncryptedSharedPreferences file.
     */
    private fun readEncryptedPrefs(prefsName: String): Map<String, Any> {
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
        return encryptedPrefs.all.toMap()
    }

    /**
     * Parses the header and returns format information.
     * Legacy v1: first 16 bytes are the IV, then CBC ciphertext. No magic, no tag.
     * Version 2: starts with magic "ILB2", followed by versioned header + GCM tag.
     */
    private fun parseHeader(allBytes: ByteArray): ParsedHeader {
        require(allBytes.size >= 4) { "Backup file too small to contain a header" }

        val magic = allBytes.copyOfRange(0, 4)
        if (!arraysEqual(magic, MAGIC)) {
            // Legacy format version 1: no magic header
            require(allBytes.size >= LEGACY_IV_LENGTH) { "Legacy backup too small for IV" }
            val iv = allBytes.copyOfRange(0, LEGACY_IV_LENGTH)
            return ParsedHeader(
                isLegacy = true,
                salt = LEGACY_SALT,
                iterations = LEGACY_PBKDF2_ITERATIONS,
                nonce = iv,
                tag = null,
                ciphertextOffset = LEGACY_IV_LENGTH
            )
        }

        // Version 2 header
        require(allBytes.size >= 13) { "Version-2 backup header truncated" }
        val version = bytesToIntBE(allBytes.copyOfRange(4, 6))
        require(version == BACKUP_VERSION) { "Unsupported backup version: $version" }

        val algorithm = allBytes[6].toInt() and 0xFF
        require(algorithm == ALGORITHM_ID) { "Unsupported algorithm: $algorithm" }

        val kdf = allBytes[7].toInt() and 0xFF
        require(kdf == KDF_ID) { "Unsupported KDF: $kdf" }

        val iterations = bytesToIntBE(allBytes.copyOfRange(8, 12))
        require(iterations > 0 && iterations <= 5_000_000) { "Invalid KDF iterations: $iterations" }

        val saltLen = allBytes[12].toInt() and 0xFF
        require(saltLen in 1..128) { "Invalid salt length: $saltLen" }
        require(allBytes.size >= 13 + saltLen) { "Header truncated reading salt" }
        val salt = allBytes.copyOfRange(13, 13 + saltLen)

        var offset = 13 + saltLen
        val nonceLen = allBytes[offset].toInt() and 0xFF
        offset += 1
        require(nonceLen in 1..32) { "Invalid nonce length: $nonceLen" }
        require(allBytes.size >= offset + nonceLen) { "Header truncated reading nonce" }
        val nonce = allBytes.copyOfRange(offset, offset + nonceLen)
        offset += nonceLen

        val tagLen = bytesToIntBE(allBytes.copyOfRange(offset, offset + 2))
        offset += 2
        require(tagLen == TAG_LENGTH) { "Invalid auth tag length: $tagLen" }
        require(allBytes.size >= offset + tagLen) { "Header truncated reading auth tag" }
        val tag = allBytes.copyOfRange(offset, offset + tagLen)
        offset += tagLen

        require(allBytes.size > offset) { "Backup contains no encrypted payload" }

        return ParsedHeader(
            isLegacy = false,
            salt = salt,
            iterations = iterations,
            nonce = nonce,
            tag = tag,
            ciphertextOffset = offset
        )
    }

    /**
     * Derives a 256-bit key from a password using PBKDF2-HMAC-SHA256.
     */
    private fun deriveKeyFromPassword(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val keyFactory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return keyFactory.generateSecret(keySpec).encoded
    }

    /**
     * AES-256-GCM encrypt: returns (ciphertext, authTag). The GCM provider appends the
     * tag to the ciphertext, so we split them apart for explicit storage in the header.
     */
    private fun encryptGcm(plaintext: ByteArray, key: ByteArray, nonce: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH * 8, nonce))
        val encrypted = cipher.doFinal(plaintext)
        require(encrypted.size > TAG_LENGTH) { "GCM output too short to contain auth tag" }
        val tag = encrypted.copyOfRange(encrypted.size - TAG_LENGTH, encrypted.size)
        val ciphertext = encrypted.copyOfRange(0, encrypted.size - TAG_LENGTH)
        return Pair(ciphertext, tag)
    }

    /**
     * AES-256-GCM decrypt. Verifies the auth tag; throws on tampered/corrupt data.
     */
    private fun decryptGcm(ciphertext: ByteArray, tag: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH * 8, nonce))
        // GCM expects ciphertext||tag concatenated for doFinal
        val full = ByteArray(ciphertext.size + tag.size)
        System.arraycopy(ciphertext, 0, full, 0, ciphertext.size)
        System.arraycopy(tag, 0, full, ciphertext.size, tag.size)
        return cipher.doFinal(full)
    }

    /**
     * Legacy AES/CBC/PKCS5Padding decrypt (format version 1). No authentication.
     */
    private fun decryptCbc(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(ciphertext)
    }

    /**
     * Builds the unencrypted version-2 header.
     */
    private fun buildHeader(salt: ByteArray, nonce: ByteArray, tag: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.write(MAGIC)
        baos.write(intToBytesBE(BACKUP_VERSION, 2))
        baos.write(ALGORITHM_ID.toByte())
        baos.write(KDF_ID.toByte())
        baos.write(intToBytesBE(PBKDF2_ITERATIONS, 4))
        baos.write(salt.size.toByte())
        baos.write(salt)
        baos.write(nonce.size.toByte())
        baos.write(nonce)
        baos.write(intToBytesBE(tag.size, 2))
        baos.write(tag)
        return baos.toByteArray()
    }

    /**
     * Parses a ZIP byte array into in-memory backup entries.
     */
    private fun parseZip(zipBytes: ByteArray): BackupEntries {
        var manifest: String? = null
        var databaseBytes: ByteArray? = null
        val preferences = mutableMapOf<String, Map<String, Any>>()

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                val bytes = zip.readFully()
                when {
                    name == MANIFEST_FILE -> manifest = String(bytes, Charsets.UTF_8)
                    name == DB_NAME -> databaseBytes = bytes
                    name.endsWith(".json") -> {
                        val prefsName = name.removeSuffix(".json")
                        val json = JSONObject(String(bytes, Charsets.UTF_8))
                        val map = mutableMapOf<String, Any>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            map[key] = json.get(key)
                        }
                        preferences[prefsName] = map
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        require(manifest != null) { "Backup is missing manifest" }
        require(databaseBytes != null) { "Backup is missing database" }

        return BackupEntries(manifest = manifest, databaseBytes = databaseBytes, preferences = preferences.toMap())
    }

    /**
     * Validates the manifest JSON before any state is mutated.
     */
    private fun validateManifest(manifestJson: String) {
        require(manifestJson.isNotBlank()) { "Manifest is blank" }
        require(manifestJson.contains("\"version\":")) { "Manifest missing version field" }
        require(manifestJson.contains("\"app_name\"")) { "Manifest missing app_name field" }
    }

    /**
     * Applies the parsed backup atomically. Preferences are applied first so the SQLCipher
     * passphrase matches the restored database; then the database file is replaced with
     * rollback support. On any failure both the database and preferences are rolled back.
     */
    private fun applyAtomically(context: Context, entries: BackupEntries) {
        val dbFile = context.getDatabasePath(DB_NAME)
        val dbDir = dbFile.parentFile
        dbDir?.mkdirs()

        // Snapshot current preferences for rollback
        val oldPrefs = snapshotPrefs()

        // Close and reset the Room instance so the database file can be replaced
        AppDatabase.closeAndReset()

        val rollbackFile = File(dbDir, "${DB_NAME}.rollback.bak")
        var rollbackAvailable = false
        if (dbFile.exists()) {
            dbFile.copyTo(rollbackFile, overwrite = true)
            rollbackAvailable = true
        }

        try {
            // Apply preferences first so the SQLCipher passphrase matches the restored DB
            applyPrefs(entries.preferences)

            // Write new database to a temp file, then atomically replace the live file
            val tempFile = File(dbDir, "${DB_NAME}.tmp")
            tempFile.outputStream().use { out -> out.write(entries.databaseBytes) }
            tempFile.moveTo(dbFile, overwrite = true)

            // Reopen and force the database to open (SQLCipher decrypts on first access)
            // to verify the restored database is valid and decryptable.
            val db = AppDatabase.getDatabase(context)
            try {
                db.runInTransaction { }
            } finally {
                db.close()
            }
            AppDatabase.closeAndReset()
        } catch (e: Exception) {
            // Rollback database file
            if (rollbackAvailable && rollbackFile.exists()) {
                rollbackFile.moveTo(dbFile, overwrite = true)
            } else {
                dbFile.delete()
            }
            // Rollback preferences
            applyPrefs(oldPrefs)
            AppDatabase.closeAndReset()
            throw e
        } finally {
            rollbackFile.delete()
        }
    }

    /**
     * Snapshots all backup-relevant SharedPreferences into memory.
     */
    private fun snapshotPrefs(): Map<String, Map<String, Any>> {
        return PREFS_NAMES.associate { (name, _) -> name to readPrefsMap(name) }
    }

    private fun readPrefsMap(prefsName: String): Map<String, Any> {
        return try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.all.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Overwrites SharedPreferences with the provided maps (clear + write).
     */
    private fun applyPrefs(preferences: Map<String, Map<String, Any>>) {
        preferences.forEach { (prefsName, entries) ->
            try {
                val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.clear()
                entries.forEach { (key, value) ->
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                        is Boolean -> editor.putBoolean(key, value)
                        else -> editor.putString(key, value.toString())
                    }
                }
                editor.apply()
            } catch (e: Exception) {
                logW("Failed to apply preferences: $prefsName", e)
                throw e
            }
        }
    }

    /**
     * Gets the size of the database file in bytes.
     */
    fun getDatabaseSize(): Long {
        val dbFile = context.getDatabasePath(DB_NAME)
        return if (dbFile.exists()) dbFile.length() else 0L
    }

    // ---------- logging helpers (operation-level only; no secrets/financial data) ----------
    private fun logD(msg: String, t: Throwable? = null) {
        android.util.Log.d(TAG, msg, t)
    }

    private fun logW(msg: String, t: Throwable? = null) {
        android.util.Log.w(TAG, msg, t)
    }

    private fun logI(msg: String) {
        android.util.Log.i(TAG, msg)
    }

    private fun logE(msg: String, t: Throwable? = null) {
        android.util.Log.e(TAG, msg, t)
    }

    // ---------- byte helpers ----------
    private fun arraysEqual(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (a[i] != b[i]) return false
        }
        return true
    }

    private fun bytesToIntBE(bytes: ByteArray): Int {
        var value = 0
        for (b in bytes) {
            value = value shl 8 or (b.toInt() and 0xFF)
        }
        return value
    }

    private fun intToBytesBE(value: Int, length: Int): ByteArray {
        val bytes = ByteArray(length)
        for (i in 0 until length) {
            bytes[length - 1 - i] = ((value shr (i * 8)) and 0xFF).toByte()
        }
        return bytes
    }

    private fun clearByteArray(array: ByteArray) {
        for (i in array.indices) {
            array[i] = 0
        }
    }

    /**
     * Reads the entire stream into a byte array.
     */
    private fun InputStream.readFully(): ByteArray {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var read = read(buffer)
        while (read != -1) {
            baos.write(buffer, 0, read)
            read = read(buffer)
        }
        return baos.toByteArray()
    }
}
