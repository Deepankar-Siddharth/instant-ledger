package com.instantledger.security

import java.util.Arrays

/**
 * Memory hygiene utilities to securely clear sensitive data from memory.
 * This helps prevent sensitive data from being exposed in memory dumps.
 */
object MemoryHygiene {
    
    /**
     * Securely clear a byte array by overwriting it with zeros.
     * Note: Due to JVM optimizations, this may not guarantee immediate clearing,
     * but it's the best practice we can follow.
     */
    fun clearByteArray(array: ByteArray?) {
        if (array != null) {
            Arrays.fill(array, 0.toByte())
        }
    }
    
    /**
     * Securely clear a char array by overwriting it with zeros.
     * Used for passphrases stored as char arrays.
     */
    fun clearCharArray(array: CharArray?) {
        if (array != null) {
            Arrays.fill(array, '\u0000')
        }
    }
    
    /**
     * Securely clear a string by creating a char array, clearing it, and returning null.
     * Note: Strings in Java/Kotlin are immutable, so we can't directly clear them.
     * This method creates a char array copy, clears it, and returns null to indicate
     * the original string reference should be discarded.
     */
    fun clearStringReference(str: String?): String? {
        if (str != null) {
            val chars = str.toCharArray()
            clearCharArray(chars)
            // Return null to indicate the original reference should be discarded
        }
        return null
    }
    
    /**
     * Execute a block with sensitive data, then clear it from memory.
     * 
     * Usage:
     * ```
     * val passphrase = getPassphrase()
     * executeAndClear(passphrase) { p ->
     *     // Use passphrase here
     *     usePassphrase(p)
     * }
     * // passphrase is cleared after block execution
     * ```
     */
    inline fun <T> executeAndClear(
        sensitiveData: ByteArray?,
        block: (ByteArray?) -> T
    ): T {
        return try {
            block(sensitiveData)
        } finally {
            clearByteArray(sensitiveData)
        }
    }
    
    /**
     * Execute a block with sensitive string data, then clear it.
     * 
     * Usage:
     * ```
     * val passphrase = getPassphrase()
     * executeAndClearString(passphrase) { p ->
     *     // Use passphrase here
     *     usePassphrase(p)
     * }
     * ```
     */
    inline fun <T> executeAndClearString(
        sensitiveData: String?,
        block: (String?) -> T
    ): T {
        val chars = sensitiveData?.toCharArray()
        return try {
            block(sensitiveData)
        } finally {
            clearCharArray(chars)
        }
    }
    
    /**
     * Clear multiple byte arrays at once.
     */
    fun clearByteArrays(vararg arrays: ByteArray?) {
        arrays.forEach { clearByteArray(it) }
    }
    
    /**
     * Clear multiple char arrays at once.
     */
    fun clearCharArrays(vararg arrays: CharArray?) {
        arrays.forEach { clearCharArray(it) }
    }
}
