package com.instantledger.data.preferences

/**
 * Metadata for a category: predefined icon key and optional color.
 * Icons are resolved via CategoryIcons; no emoji or custom icon storage.
 */
data class CategoryMetadata(
    val name: String,
    val iconKey: String = CategoryIcons.DEFAULT_ICON_KEY,
    val color: Long? = null // Color as Long (ARGB), null means use default
) {
    /** Resolved icon key for display; always a valid predefined key. */
    fun getResolvedIconKey(): String {
        return if (CategoryIcons.isValidIconKey(iconKey)) iconKey else CategoryIcons.DEFAULT_ICON_KEY
    }

    companion object {
        fun fromJson(json: String): CategoryMetadata? {
            return try {
                val parts = json.split("|")
                if (parts.size >= 2) {
                    val name = parts[0]
                    // Legacy format (v1.0): name|icon|isEmoji|color (4 parts)
                    if (parts.size >= 4) {
                        val legacyIcon = parts[1].takeIf { it.isNotBlank() }
                        val wasEmoji = parts[2].toBoolean()
                        val colorFromLegacy = parts[3].toLongOrNull()
                        val migratedKey = CategoryIcons.migrateToIconKey(legacyIcon, wasEmoji)
                        return CategoryMetadata(name = name, iconKey = migratedKey, color = colorFromLegacy)
                    }
                    // New format (v1.1): name|iconKey|color (3 parts)
                    val iconKeyPart = parts[1].takeIf { it.isNotBlank() }
                    val colorPart = parts.getOrNull(2)?.toLongOrNull()
                    if (CategoryIcons.isValidIconKey(iconKeyPart)) {
                        CategoryMetadata(name = name, iconKey = iconKeyPart!!, color = colorPart)
                    } else {
                        // Corrupt or legacy 3-part: parts[1]=icon, parts[2]=isEmoji
                        val wasEmoji = parts.getOrNull(2)?.toBoolean() ?: false
                        val migratedKey = CategoryIcons.migrateToIconKey(iconKeyPart, wasEmoji)
                        CategoryMetadata(name = name, iconKey = migratedKey, color = null)
                    }
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
                metadata.getResolvedIconKey(),
                metadata.color?.toString() ?: ""
            ).joinToString("|")
        }
    }
}
