package com.instantledger.data.preferences

import androidx.compose.ui.graphics.Color

/**
 * Metadata for a category including icon/emoji and color
 */
data class CategoryMetadata(
    val name: String,
    val icon: String? = null, // Material icon name or emoji character
    val isEmoji: Boolean = false, // true if icon is an emoji, false if Material icon name
    val color: Long? = null // Color as Long (ARGB), null means use default
) {
    fun getDisplayIcon(): String {
        return icon ?: getDefaultIconForCategory(name)
    }
    
    companion object {
        private fun getDefaultIconForCategory(categoryName: String): String {
            return when (categoryName.lowercase()) {
                "work" -> "💼"
                "main" -> "🏠"
                "ignore" -> "🚫"
                "personal" -> "👤"
                "food" -> "🍔"
                "transport" -> "🚗"
                "shopping" -> "🛒"
                "entertainment" -> "🎬"
                "health" -> "🏥"
                "bills" -> "📄"
                else -> "📦"
            }
        }
        
        fun fromJson(json: String): CategoryMetadata? {
            return try {
                val parts = json.split("|")
                if (parts.size >= 2) {
                    CategoryMetadata(
                        name = parts[0],
                        icon = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
                        isEmoji = parts.getOrNull(2)?.toBoolean() ?: false,
                        color = parts.getOrNull(3)?.toLongOrNull()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        fun toJson(metadata: CategoryMetadata): String {
            return listOf(
                metadata.name,
                metadata.icon ?: "",
                metadata.isEmoji.toString(),
                metadata.color?.toString() ?: ""
            ).joinToString("|")
        }
    }
}
