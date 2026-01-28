package com.instantledger.data.database.dao

import androidx.room.*
import com.instantledger.data.database.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories WHERE is_active = 1 ORDER BY name ASC")
    fun getAllActiveCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?
    
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Query("UPDATE categories SET is_active = 0 WHERE id = :id")
    suspend fun deactivateCategory(id: String)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)
}
