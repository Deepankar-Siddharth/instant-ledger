package com.instantledger.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.instantledger.data.model.Category
import com.instantledger.data.model.CategorySemantic

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String, // UUID
    val name: String,
    val semantic: String, // Stored as string, converted from enum
    val created_at: Long,
    val updated_at: Long,
    val is_active: Int // 1 = true, 0 = false
) {
    fun toDomain(): Category {
        return Category(
            id = id,
            name = name,
            semantic = CategorySemantic.valueOf(semantic),
            createdAt = created_at,
            updatedAt = updated_at,
            isActive = is_active == 1
        )
    }
    
    companion object {
        fun fromDomain(category: Category): CategoryEntity {
            return CategoryEntity(
                id = category.id,
                name = category.name,
                semantic = category.semantic.name,
                created_at = category.createdAt,
                updated_at = category.updatedAt,
                is_active = if (category.isActive) 1 else 0
            )
        }
    }
}
