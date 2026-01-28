package com.instantledger.data.preferences

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class CategoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "instant_ledger_categories",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_CATEGORY_METADATA = "category_metadata"
        private val DEFAULT_CATEGORIES = listOf("Main", "Work", "Ignore")
    }
    
    fun getCategories(): List<String> {
        val categoriesString = prefs.getString(KEY_CATEGORIES, null)
        return if (categoriesString != null) {
            categoriesString.split(",").filter { it.isNotBlank() }
        } else {
            DEFAULT_CATEGORIES
        }
    }
    
    fun saveCategories(categories: List<String>) {
        prefs.edit()
            .putString(KEY_CATEGORIES, categories.joinToString(","))
            .apply()
    }
    
    fun addCategory(category: String) {
        val currentCategories = getCategories().toMutableList()
        if (!currentCategories.contains(category.trim())) {
            currentCategories.add(category.trim())
            saveCategories(currentCategories)
        }
    }
    
    fun removeCategory(category: String) {
        val currentCategories = getCategories().toMutableList()
        currentCategories.remove(category)
        saveCategories(currentCategories)
    }
    
    fun updateCategory(oldCategory: String, newCategory: String) {
        val currentCategories = getCategories().toMutableList()
        val index = currentCategories.indexOf(oldCategory)
        if (index != -1) {
            // Preserve metadata if name changes
            val oldMetadata = getCategoryMetadata(oldCategory)
            currentCategories[index] = newCategory.trim()
            saveCategories(currentCategories)
            
            // Update metadata with new name
            if (oldCategory != newCategory.trim()) {
                val newMetadata = oldMetadata.copy(name = newCategory.trim())
                updateCategoryMetadata(oldCategory, newMetadata)
            }
        }
    }
    
    fun clearAllCategories() {
        prefs.edit().clear().apply()
        // Restore default categories
        saveCategories(DEFAULT_CATEGORIES)
    }
    
    fun getAllCategories(): List<String> = getCategories()
    
    // Category Metadata Methods
    fun getCategoryMetadata(categoryName: String): CategoryMetadata {
        val metadataJson = prefs.getString(KEY_CATEGORY_METADATA, null)
        if (metadataJson != null) {
            try {
                val json = JSONObject(metadataJson)
                val categoryJson = json.optJSONObject(categoryName)
                if (categoryJson != null) {
                    return CategoryMetadata(
                        name = categoryName,
                        icon = categoryJson.optString("icon", null).takeIf { it.isNotBlank() },
                        isEmoji = categoryJson.optBoolean("isEmoji", false),
                        color = categoryJson.optLong("color", -1).takeIf { it != -1L }
                    )
                }
            } catch (e: Exception) {
                // Fall through to default
            }
        }
        return CategoryMetadata(name = categoryName)
    }
    
    fun getAllCategoriesWithMetadata(): List<CategoryMetadata> {
        return getCategories().map { getCategoryMetadata(it) }
    }
    
    fun saveCategoryMetadata(metadata: CategoryMetadata) {
        val metadataJson = prefs.getString(KEY_CATEGORY_METADATA, null)
        val json = if (metadataJson != null) {
            try {
                JSONObject(metadataJson)
            } catch (e: Exception) {
                JSONObject()
            }
        } else {
            JSONObject()
        }
        
        val categoryJson = JSONObject().apply {
            if (metadata.icon != null) {
                put("icon", metadata.icon)
            }
            put("isEmoji", metadata.isEmoji)
            if (metadata.color != null) {
                put("color", metadata.color)
            }
        }
        
        json.put(metadata.name, categoryJson)
        prefs.edit().putString(KEY_CATEGORY_METADATA, json.toString()).apply()
    }
    
    fun updateCategoryMetadata(oldName: String, newMetadata: CategoryMetadata) {
        // Remove old metadata if name changed
        if (oldName != newMetadata.name) {
            val metadataJson = prefs.getString(KEY_CATEGORY_METADATA, null)
            if (metadataJson != null) {
                try {
                    val json = JSONObject(metadataJson)
                    json.remove(oldName)
                    prefs.edit().putString(KEY_CATEGORY_METADATA, json.toString()).apply()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        saveCategoryMetadata(newMetadata)
    }
    
    fun deleteCategoryMetadata(categoryName: String) {
        val metadataJson = prefs.getString(KEY_CATEGORY_METADATA, null)
        if (metadataJson != null) {
            try {
                val json = JSONObject(metadataJson)
                json.remove(categoryName)
                prefs.edit().putString(KEY_CATEGORY_METADATA, json.toString()).apply()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
