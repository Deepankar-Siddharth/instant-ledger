package com.instantledger.data.database.dao

import androidx.room.*
import com.instantledger.data.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    
    @Query("SELECT * FROM goals ORDER BY created_at DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>
    
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long
    
    @Update
    suspend fun updateGoal(goal: GoalEntity)
    
    @Delete
    suspend fun deleteGoal(goal: GoalEntity)
    
    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
    
    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()
}
