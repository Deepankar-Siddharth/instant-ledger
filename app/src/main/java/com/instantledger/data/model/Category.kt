package com.instantledger.data.model

import java.util.UUID

/**
 * Category model with versioning support.
 * Uses UUID to track categories even when names change.
 */
data class Category(
    val id: String, // UUID - never changes
    val name: String, // Current name - can change
    val semantic: CategorySemantic = CategorySemantic.PERSONAL,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
) {
    companion object {
        fun create(name: String, semantic: CategorySemantic = CategorySemantic.PERSONAL): Category {
            val now = System.currentTimeMillis()
            return Category(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                semantic = semantic,
                createdAt = now,
                updatedAt = now,
                isActive = true
            )
        }
    }
}

/**
 * Category semantics define the meaning/purpose of a category.
 * This allows different forecasting logic and UI behavior.
 */
enum class CategorySemantic {
    PERSONAL,   // Personal expenses (default)
    SHARED,     // Shared/household expenses
    SAVINGS,    // Savings goals
    BUSINESS,   // Business expenses
    IGNORE      // Transactions to ignore in analytics
}
