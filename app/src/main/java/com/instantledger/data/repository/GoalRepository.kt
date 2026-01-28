package com.instantledger.data.repository

import com.instantledger.data.database.dao.GoalDao
import com.instantledger.data.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun getAllGoals(): Flow<List<GoalEntity>> {
        return goalDao.getAllGoals()
    }
    
    suspend fun getGoalById(id: Long): GoalEntity? {
        return goalDao.getGoalById(id)
    }
    
    suspend fun insertGoal(goal: GoalEntity): Long {
        return goalDao.insertGoal(goal)
    }
    
    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
    }
    
    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
    }
    
    suspend fun deleteGoalById(id: Long) {
        goalDao.deleteGoalById(id)
    }
    
    suspend fun addAmountToGoal(goalId: Long, amount: Double) {
        val goal = goalDao.getGoalById(goalId)
        if (goal != null) {
            val updatedGoal = goal.copy(
                currentAmount = goal.currentAmount + amount,
                updatedAt = System.currentTimeMillis()
            )
            goalDao.updateGoal(updatedGoal)
        }
    }
}
