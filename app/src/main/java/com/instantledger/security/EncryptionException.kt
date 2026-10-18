package com.instantledger.security

/**
 * Exception thrown when database encryption fails.
 * This is a critical error - the app cannot proceed without encryption.
 */
class EncryptionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    companion object {
        const val ERROR_CODE_SQLCIPHER_NOT_FOUND = "SQLCIPHER_NOT_FOUND"
        const val ERROR_CODE_SQLCIPHER_LOAD_FAILED = "SQLCIPHER_LOAD_FAILED"
        const val ERROR_CODE_FACTORY_CREATION_FAILED = "FACTORY_CREATION_FAILED"
        const val ERROR_CODE_PASSPHRASE_GENERATION_FAILED = "PASSPHRASE_GENERATION_FAILED"
    }
    
    val errorCode: String = when {
        message.contains("not found", ignoreCase = true) -> ERROR_CODE_SQLCIPHER_NOT_FOUND
        message.contains("load", ignoreCase = true) -> ERROR_CODE_SQLCIPHER_LOAD_FAILED
        message.contains("factory", ignoreCase = true) -> ERROR_CODE_FACTORY_CREATION_FAILED
        message.contains("passphrase", ignoreCase = true) -> ERROR_CODE_PASSPHRASE_GENERATION_FAILED
        else -> "UNKNOWN"
    }
}
