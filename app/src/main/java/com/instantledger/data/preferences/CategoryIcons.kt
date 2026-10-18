package com.instantledger.data.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Predefined category icon set. All category icons are drawn from this list.
 * Icons are bundled Material Icons; no custom emoji or external drawables.
 */
object CategoryIcons {

    /** Default icon key when none is set or migration finds no match */
    const val DEFAULT_ICON_KEY = "other"

    /**
     * All valid icon keys in display order for the icon picker grid.
     */
    val ALL_ICON_KEYS: List<String> = listOf(
        "home",
        "work",
        "shopping",
        "food",
        "transport",
        "health",
        "education",
        "entertainment",
        "travel",
        "money",
        "bank",
        "card",
        "savings",
        "receipt",
        "other"
    )

    /**
     * Maps icon key to Material Icon ImageVector. Returns the "other" icon for unknown keys.
     */
    fun getImageVector(iconKey: String?): ImageVector {
        return when (iconKey?.lowercase()?.trim()) {
            "home" -> Icons.Default.Home
            "work" -> Icons.Default.Work
            "shopping" -> Icons.Default.ShoppingCart
            "food" -> Icons.Default.Restaurant
            "transport" -> Icons.Default.DirectionsCar
            "health" -> Icons.Default.LocalHospital
            "education" -> Icons.Default.School
            "entertainment" -> Icons.Default.SportsEsports
            "travel" -> Icons.Default.Flight
            "money" -> Icons.Default.AttachMoney
            "bank" -> Icons.Default.AccountBalance
            "card" -> Icons.Default.CreditCard
            "savings" -> Icons.Default.Savings
            "receipt" -> Icons.Default.Receipt
            else -> Icons.Default.Category // "other"
        }
    }

    /**
     * Returns true if the given key is in the predefined set.
     */
    fun isValidIconKey(iconKey: String?): Boolean {
        return iconKey != null && ALL_ICON_KEYS.contains(iconKey.lowercase().trim())
    }

    /**
     * Maps legacy stored value (emoji character or old Material icon name) to a predefined iconKey.
     * Used when migrating existing CategoryMetadata from pre-1.1.0.
     */
    fun migrateToIconKey(legacyIcon: String?, wasEmoji: Boolean): String {
        if (legacyIcon.isNullOrBlank()) return DEFAULT_ICON_KEY
        val s = legacyIcon.trim()
        if (wasEmoji) {
            return when (s) {
                "🏠", "🏡" -> "home"
                "💼", "👔" -> "work"
                "🛒", "🛍️" -> "shopping"
                "🍔", "🍕", "☕", "🍽️" -> "food"
                "🚗", "⛽", "🚌", "🚕", "🛵" -> "transport"
                "🏥", "💊", "❤️" -> "health"
                "🏫", "📚", "✏️" -> "education"
                "🎬", "🎮", "🎯", "🎲" -> "entertainment"
                "✈️", "🏨", "🌍", "🧳" -> "travel"
                "💰", "💵", "💴", "💶", "💷" -> "money"
                "🏦" -> "bank"
                "💳", "📇" -> "card"
                "💵", "🪙" -> "savings"
                "🧾", "📄", "📋" -> "receipt"
                "🚫", "📦", "📌" -> "other"
                else -> DEFAULT_ICON_KEY
            }
        }
        // Old Material icon name (e.g. "Home", "ShoppingCart")
        val normalized = s.replace(" ", "").lowercase()
        if (ALL_ICON_KEYS.contains(normalized)) return normalized
        return when (normalized) {
            "home" -> "home"
            "work" -> "work"
            "shoppingcart" -> "shopping"
            "restaurant" -> "food"
            "localgasstation", "directionscar" -> "transport"
            "localhospital" -> "health"
            "school" -> "education"
            "sportsesports" -> "entertainment"
            "flight", "hotel" -> "travel"
            "attachmoney" -> "money"
            "accountbalance" -> "bank"
            "creditcard" -> "card"
            "savings" -> "savings"
            "receipt" -> "receipt"
            "category" -> "other"
            else -> DEFAULT_ICON_KEY
        }
    }
}
