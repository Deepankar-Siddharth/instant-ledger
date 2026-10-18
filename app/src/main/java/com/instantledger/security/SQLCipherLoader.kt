package com.instantledger.security

import android.content.Context
import android.util.Log

/**
 * Helper class to ensure SQLCipher classes are included in the APK.
 * This class references SQLCipher at compile time to prevent R8 from removing it.
 * 
 * IMPORTANT: Even though we use reflection at runtime, we need compile-time
 * references to ensure the classes are included in the DEX file.
 */
object SQLCipherLoader {
    
    /**
     * Load SQLCipher native libraries.
     * Must be called before using SQLCipher.
     */
    fun loadNativeLibraries(context: Context) {
        try {
            // NOTE: Correct SQLCipher class is net.sqlcipher.database.SQLiteDatabase
            val sqliteDatabaseClass = Class.forName("net.sqlcipher.database.SQLiteDatabase")
            val loadLibsMethod = sqliteDatabaseClass.getMethod("loadLibs", Context::class.java)
            loadLibsMethod.invoke(null, context)
            Log.d("InstantLedger", "SQLCipherLoader: Native libraries loaded successfully")
        } catch (e: ClassNotFoundException) {
            Log.e("InstantLedger", "SQLCipherLoader: SQLCipher class not found", e)
            throw EncryptionException(
                "SQLCipher encryption library not found. " +
                "The database cannot be encrypted without SQLCipher. " +
                "Please ensure 'net.zetetic:android-database-sqlcipher:4.5.4@aar' is included in dependencies. " +
                "For a finance app, encryption is mandatory for user data protection.",
                e
            )
        } catch (e: Exception) {
            Log.e("InstantLedger", "SQLCipherLoader: Error loading native libraries", e)
            throw EncryptionException(
                "Failed to load SQLCipher native libraries. " +
                "This is required for database encryption. " +
                "Error: ${e.message}",
                e
            )
        }
    }
}
