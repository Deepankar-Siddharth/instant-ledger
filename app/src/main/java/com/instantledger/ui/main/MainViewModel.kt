package com.instantledger.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantledger.data.model.Transaction
import com.instantledger.data.model.TransactionType
import com.instantledger.data.model.PaymentMode
import com.instantledger.data.model.EntryType
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.data.repository.TripRepository
import com.instantledger.data.repository.MerchantRepository
import com.instantledger.notification.TransactionNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Filter state for global transaction filtering across all tabs.
 * No debit/credit filter; calculations use raw amounts (incoming = sum(amount>0), outgoing = sum(amount<0)).
 */
data class FilterState(
    val approvalStatus: ApprovalStatusFilter? = null, // null = all, PENDING = only pending, APPROVED = only approved
    val selectedCategories: Set<String> = emptySet(), // Empty = all categories, non-empty = only selected
    val paymentModes: Set<PaymentMode> = emptySet() // Empty = all modes, non-empty = only selected
) {
    fun hasActiveFilters(): Boolean {
        return approvalStatus != null ||
                selectedCategories.isNotEmpty() ||
                paymentModes.isNotEmpty()
    }
    
    fun clear(): FilterState {
        return FilterState()
    }
}

enum class ApprovalStatusFilter {
    PENDING, APPROVED
}

@HiltViewModel
class MainViewModel @Inject constructor(
    val transactionRepository: TransactionRepository,
    private val tripRepository: TripRepository,
    val merchantRepository: MerchantRepository,
    private val notificationManager: TransactionNotificationManager
) : ViewModel() {
    
    // Global filter state - persists across tab switches
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
    }
    
    fun clearFilters() {
        _filterState.value = FilterState()
    }
    
    val transactions: StateFlow<List<Transaction>> = transactionRepository
        .getAllTransactions()
        .onEach { list ->
            android.util.Log.d("InstantLedger", "MainViewModel: Transactions updated - count: ${list.size}")
            if (list.isNotEmpty()) {
                // Don't log sensitive transaction amounts
                android.util.Log.d("InstantLedger", "MainViewModel: Latest transaction - ID: ${list.first().id}, Approved: ${list.first().isApproved}")
            }
            
            // Cancel notifications when pending count reaches zero
            // Pending = transactions without category OR not approved
            val pendingCount = list.count { 
                it.category.isNullOrBlank() || !it.isApproved 
            }
            if (pendingCount == 0) {
                notificationManager.cancelAllNotifications()
            } else {
                // Update grouped summary if needed
                notificationManager.updateGroupedSummary()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Calculate total owed per participant for a given trip.
     */
    suspend fun getTripBalances(tripId: Long): Map<com.instantledger.data.model.TripParticipant, Double> {
        return tripRepository.getTripBalances(tripId)
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
    
    fun calculateProjectedSpend(timePeriod: TimePeriod): Flow<ProjectedSpendData> {
        return combine(transactions, filterState) { allTransactions, filters ->
            val calendar = Calendar.getInstance()
            val now = System.currentTimeMillis()

            // Time range filter
            val startTime = when (timePeriod) {
                TimePeriod.TODAY -> {
                    calendar.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                TimePeriod.WEEKLY -> {
                    calendar.apply {
                        set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                TimePeriod.MONTHLY -> {
                    calendar.apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                TimePeriod.YEARLY -> {
                    calendar.apply {
                        set(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
            }

            var dataset = allTransactions.filter { it.timestamp in startTime..now }

            // Approval status filter
            dataset = when (filters.approvalStatus) {
                ApprovalStatusFilter.PENDING -> dataset.filter { !it.isApproved }
                ApprovalStatusFilter.APPROVED -> dataset.filter { it.isApproved }
                null -> {
                    // Default behavior: Today => all, others => approved only
                    if (timePeriod == TimePeriod.TODAY) dataset
                    else dataset.filter { it.isApproved }
                }
            }

            // Category filter
            if (filters.selectedCategories.isNotEmpty()) {
                dataset = dataset.filter { tx ->
                    val catName = tx.category ?: tx.categoryNameSnapshot ?: "Uncategorized"
                    filters.selectedCategories.contains(catName)
                }
            }

            // Payment mode filter
            if (filters.paymentModes.isNotEmpty()) {
                dataset = dataset.filter { tx -> filters.paymentModes.contains(tx.paymentMode) }
            }

            // Totals from raw stored amounts: outgoing = sum(debit amounts), incoming = sum(credit amounts)
            val totalDebit = dataset
                .filter { it.transactionType == TransactionType.DEBIT }
                .sumOf { it.amount }
            val totalCredit = dataset
                .filter { it.transactionType == TransactionType.CREDIT }
                .sumOf { it.amount }
            val net = totalCredit - totalDebit

            val title = when {
                filters.selectedCategories.size == 1 ->
                    "${filters.selectedCategories.first()} · Summary"
                filters.selectedCategories.size > 1 ->
                    "Selected Categories · Summary"
                else -> "All Transactions · Summary"
            }

            // Category totals (net per category) respecting all active filters
            val categoryTotals: Map<String, Double> = dataset
                .groupBy { it.category ?: it.categoryNameSnapshot ?: "Uncategorized" }
                .mapValues { (_, list) ->
                    val totalDebit = list
                        .filter { it.transactionType == TransactionType.DEBIT }
                        .sumOf { it.amount }
                    val totalCredit = list
                        .filter { it.transactionType == TransactionType.CREDIT }
                        .sumOf { it.amount }
                    totalCredit - totalDebit
                }

            ProjectedSpendData(
                title = title,
                total = net,
                dailyAverage = 0.0,
                categoryTotals = categoryTotals,
                hasData = dataset.isNotEmpty(),
                isCategorySummary = filters.selectedCategories.isNotEmpty(),
                debitTotal = totalDebit,
                creditTotal = totalCredit
            )
        }
    }
}

data class ProjectedSpendData(
    val title: String,
    val total: Double,
    val dailyAverage: Double,
    val categoryTotals: Map<String, Double>,
    val hasData: Boolean,
    val isCategorySummary: Boolean = false,
    val debitTotal: Double = 0.0,
    val creditTotal: Double = 0.0
)
