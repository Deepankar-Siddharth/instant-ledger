package com.instantledger.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import com.instantledger.ui.components.TintedCircleIconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.LaunchedEffect
import com.instantledger.ui.dashboard.DashboardScreen
import com.instantledger.data.model.Transaction
import com.instantledger.data.preferences.CategoryManager
import com.instantledger.data.preferences.IgnoredHashesStore
import com.instantledger.data.repository.TransactionRepository
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

enum class TimePeriod {
    TODAY, WEEKLY, MONTHLY, YEARLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    transactions: List<Transaction>,
    onAddTransaction: (com.instantledger.data.model.PaymentMode) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    transactionRepository: TransactionRepository? = null,
    viewModel: MainViewModel = hiltViewModel(),
    settingsPreferences: com.instantledger.data.preferences.EncryptedSettingsPreferences,
    navController: NavController,
    openPendingSection: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var lastBackPress by remember { mutableStateOf(0L) }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    // Handle opening to pending section from notification
    LaunchedEffect(openPendingSection) {
        if (openPendingSection) {
            selectedTabIndex = 0 // Ensure Today tab is selected
            // The pending section will automatically be visible if there are pending transactions
        }
    }
    
    // Handle back button on root screen (exit confirmation)
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPress < 2000) {
            // Second back press within 2 seconds - exit app
            (context as? android.app.Activity)?.finish()
        } else {
            // First back press - show toast
            lastBackPress = currentTime
            android.widget.Toast.makeText(
                context,
                "Press back again to exit",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Calculate projected spend based on selected tab
    val currentTimePeriod = TimePeriod.values()[selectedTabIndex]
    val projectedSpendData by viewModel.calculateProjectedSpend(currentTimePeriod)
        .collectAsState(initial = null)
    
    val filteredTransactions = remember(transactions, selectedTabIndex) {
        val period = TimePeriod.values()[selectedTabIndex]
        val periodFiltered = filterTransactionsByPeriod(transactions, period)
        
        // For Weekly/Monthly/Yearly: only show approved transactions
        // For Today: show all transactions (including pending/unapproved)
        if (period == TimePeriod.TODAY) {
            periodFiltered
        } else {
            periodFiltered.filter { it.isApproved }
        }
    }
    
    val periodTitle = remember(selectedTabIndex) {
        when (TimePeriod.values()[selectedTabIndex]) {
            TimePeriod.TODAY -> "Today's Transactions"
            TimePeriod.WEEKLY -> "This Week's Transactions"
            TimePeriod.MONTHLY -> "This Month's Transactions"
            TimePeriod.YEARLY -> "This Year's Transactions"
        }
    }
    
    // Get filter state to determine app bar title
    val filterState by viewModel.filterState.collectAsState()
    
    // Dynamic app bar title based on category filter
    val appBarTitle = remember(filterState.selectedCategories) {
        when {
            // Exactly one category selected - show category name
            filterState.selectedCategories.size == 1 -> filterState.selectedCategories.first()
            // No category filter or multiple categories - show default title
            else -> "Instant Ledger"
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
                // TopAppBar with dynamic title based on category filter
                TopAppBar(
                    title = { 
                        Text(
                            text = appBarTitle,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = "Filter"
                            )
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
                
                // Global Insight Card (above tabs, visible on all tabs)
                projectedSpendData?.let { data ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        com.instantledger.ui.dashboard.GlobalInsightCard(
                            projectedSpendData = data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // ScrollableTabRow for tabs (sleeker look)
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Today") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Weekly") }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Monthly") }
                    )
                    Tab(
                        selected = selectedTabIndex == 3,
                        onClick = { selectedTabIndex = 3 },
                        text = { Text("Yearly") }
                    )
                }
                
                // Icon-Only Manual Action Bar (only on Today tab) – tinted circular icons
                if (selectedTabIndex == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TintedCircleIconButton(
                            onClick = { onAddTransaction(com.instantledger.data.model.PaymentMode.CASH) },
                            imageVector = Icons.Filled.Payments,
                            contentDescription = "Cash",
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        TintedCircleIconButton(
                            onClick = { onAddTransaction(com.instantledger.data.model.PaymentMode.UPI) },
                            imageVector = Icons.Filled.Smartphone,
                            contentDescription = "UPI",
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        TintedCircleIconButton(
                            onClick = { onAddTransaction(com.instantledger.data.model.PaymentMode.CARD) },
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = "Card",
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        TintedCircleIconButton(
                            onClick = { onAddTransaction(com.instantledger.data.model.PaymentMode.BANK) },
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = "Bank",
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                    }
                }
            
                DashboardScreen(
                    transactions = filteredTransactions,
                    title = periodTitle,
                    timePeriod = TimePeriod.values()[selectedTabIndex],
                    onAddTransaction = null, // Handled by action buttons in MainScreen
                    onEditTransaction = onEditTransaction,
                    onDeleteTransaction = onDeleteTransaction,
                    onQuickClassify = { tx, category ->
                        if (transactionRepository != null) {
                            uiScope.launch(Dispatchers.IO) {
                                // Category is mandatory for approval - only approve if category is provided
                                val updated = tx.copy(
                                    category = category,
                                    isApproved = category.isNotBlank(), // Only approve if category is not empty
                                    updatedAt = System.currentTimeMillis()
                                )
                                transactionRepository.updateTransaction(updated)
                            }
                        }
                    },
                    onIgnorePending = { tx ->
                        uiScope.launch(Dispatchers.IO) {
                            tx.rawTextHash?.let { hash ->
                                IgnoredHashesStore(context.applicationContext).add(hash)
                            }
                            transactionRepository?.deleteTransaction(tx)
                        }
                    },
                    onTransactionClick = { transaction ->
                        navController.navigate("transaction_detail/${transaction.id}")
                    },
                    onSettingsClick = null, // Settings is now in TopAppBar
                    projectedMonthlySpend = null, // Moved to GlobalInsightCard above tabs
                    filterState = filterState,
                    onFilterStateChange = { newState ->
                        viewModel.updateFilterState(newState)
                    },
                    merchantRepository = viewModel.merchantRepository
                )

                // Global filter bottom sheet, anchored to top-level screen
                if (showFilterSheet) {
                    com.instantledger.ui.dashboard.FilterBottomSheet(
                        onDismiss = { showFilterSheet = false },
                        filterState = filterState,
                        availableCategories = CategoryManager(context).getCategories(),
                        allTransactions = transactions,
                        onFilterStateChange = { newState ->
                            viewModel.updateFilterState(newState)
                        }
                    )
                }
    }
}

private fun filterTransactionsByPeriod(
    transactions: List<Transaction>,
    period: TimePeriod
): List<Transaction> {
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis()
    
    val startTime = when (period) {
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
    
    return transactions.filter { 
        it.timestamp >= startTime && it.timestamp <= now 
    }.sortedByDescending { it.timestamp }
}
