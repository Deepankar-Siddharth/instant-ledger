package com.instantledger.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantledger.data.database.entities.GoalEntity
import com.instantledger.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {
    
    val goals: StateFlow<List<GoalEntity>> = goalRepository
        .getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun createGoal(name: String, targetAmount: Double) {
        viewModelScope.launch {
            val goal = GoalEntity(
                name = name,
                targetAmount = targetAmount,
                currentAmount = 0.0
            )
            goalRepository.insertGoal(goal)
        }
    }
    
    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }
    
    suspend fun addAmountToGoal(goalId: Long, amount: Double) {
        goalRepository.addAmountToGoal(goalId, amount)
    }
}
