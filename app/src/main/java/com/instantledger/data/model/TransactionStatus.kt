package com.instantledger.data.model

enum class TransactionStatus {
    DETECTED,    // Auto-captured but not reviewed
    CONFIRMED,   // User accepted
    MODIFIED,    // User edited
    IGNORED      // Hidden from totals
}
