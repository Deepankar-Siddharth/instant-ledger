package com.instantledger.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import com.instantledger.ui.components.TintedCircleIconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.instantledger.data.preferences.CategoryIcons
import com.instantledger.data.model.Transaction
import com.instantledger.data.model.EntryType
import com.instantledger.data.model.TransactionType
import com.instantledger.data.model.PaymentMode
import com.instantledger.ui.main.TimePeriod
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

data class GroupedTransaction(
    val header: String,
    val transactions: List<Transaction>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    transactions: List<Transaction>,
    title: String = "Transactions",
    timePeriod: TimePeriod = TimePeriod.TODAY,
    onAddTransaction: (() -> Unit)? = null, // Optional, handled in MainScreen now
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onQuickClassify: (Transaction, String) -> Unit = { _, _ -> },
    onIgnorePending: (Transaction) -> Unit = {},
    onTransactionClick: ((Transaction) -> Unit)? = null, // Navigate to detail screen
    onSettingsClick: (() -> Unit)? = null,
    projectedMonthlySpend: Double? = null, // Forecast amount
    filterState: com.instantledger.ui.main.FilterState = com.instantledger.ui.main.FilterState(), // Global filter state
    onFilterStateChange: (com.instantledger.ui.main.FilterState) -> Unit = {}, // Callback to update filter
    merchantRepository: com.instantledger.data.repository.MerchantRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Resolve primary categories once per composition for quick pending actions
    val quickCategories = remember {
        com.instantledger.data.preferences.CategoryManager(context)
            .getCategories()
            .filter { it != "Ignore" }
    }
    val quickCat1 = quickCategories.getOrNull(0) ?: "Main"
    val quickCat2 = quickCategories.getOrNull(1) ?: "Work"
    
    // Filter transactions based on time period and approval status
    val baseFilteredTransactions = remember(transactions, timePeriod) {
        if (timePeriod == TimePeriod.TODAY) {
            // Today view shows all transactions including pending/unapproved ones
            transactions
        } else {
            // Weekly, Monthly, and Yearly views: Only show approved transactions
            // Also filter out USER_ENTERED (manual) transactions for these views
            transactions.filter { 
                it.isApproved && it.entryType != EntryType.USER_ENTERED 
            }
        }
    }
    
    // Apply global filters
    val filteredTransactions = remember(baseFilteredTransactions, filterState) {
        baseFilteredTransactions.filter { transaction ->
            val approvalMatch = when (filterState.approvalStatus) {
                com.instantledger.ui.main.ApprovalStatusFilter.PENDING -> !transaction.isApproved
                com.instantledger.ui.main.ApprovalStatusFilter.APPROVED -> transaction.isApproved
                null -> true
            }
            val categoryMatch = filterState.selectedCategories.isEmpty() ||
                    (transaction.category != null && filterState.selectedCategories.contains(transaction.category))
            val paymentModeMatch = filterState.paymentModes.isEmpty() ||
                    filterState.paymentModes.contains(transaction.paymentMode)
            approvalMatch && categoryMatch && paymentModeMatch
        }
    }
    
    // Separate approved and pending transactions for Today view
    // Pending = transactions without category OR not approved
    val (approvedTransactions, pendingTransactions) = remember(filteredTransactions, timePeriod) {
        if (timePeriod == TimePeriod.TODAY) {
            filteredTransactions.partition { 
                it.isApproved && !it.category.isNullOrBlank() // Approved AND has category
            }
        } else {
            Pair(filteredTransactions, emptyList())
        }
    }

    // Pending transactions for Today view - includes all uncategorized or unapproved transactions
    val pendingTransactionsForToday = remember(pendingTransactions, timePeriod) {
        if (timePeriod == TimePeriod.TODAY) {
            // Show all pending transactions (uncategorized or unapproved)
            pendingTransactions.filter { 
                it.category.isNullOrBlank() || !it.isApproved 
            }
        } else {
            emptyList()
        }
    }
    
    // Group approved transactions based on time period
    val groupedTransactions = remember(approvedTransactions, timePeriod) {
        when (timePeriod) {
            TimePeriod.TODAY -> listOf(GroupedTransaction("Today", approvedTransactions))
            TimePeriod.WEEKLY -> groupByDay(approvedTransactions)
            TimePeriod.MONTHLY -> groupByWeek(approvedTransactions)
            TimePeriod.YEARLY -> groupByMonth(approvedTransactions)
        }
    }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        val hasAnyTransactions = if (timePeriod == TimePeriod.TODAY) {
            approvedTransactions.isNotEmpty() || pendingTransactionsForToday.isNotEmpty()
        } else {
            filteredTransactions.isNotEmpty()
        }
        
        if (!hasAnyTransactions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No transactions found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pending approval section – Today tab only
                if (timePeriod == TimePeriod.TODAY && pendingTransactionsForToday.isNotEmpty()) {
                    item {
                        PendingApprovalHeader()
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(pendingTransactionsForToday) { transaction ->
                        PendingTransactionCard(
                            transaction = transaction,
                            category1 = quickCat1,
                            category2 = quickCat2,
                            onCategory1 = { onQuickClassify(transaction, quickCat1) },
                            onCategory2 = { onQuickClassify(transaction, quickCat2) },
                            onIgnore = { onIgnorePending(transaction) },
                            merchantRepository = merchantRepository
                        )
                    }

                    // Space between pending and approved sections
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                groupedTransactions.forEach { group ->
                    // Section Header (per-group)
                    item {
                        SectionHeader(group.header)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Transactions in this group
                    items(group.transactions) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onEdit = onEditTransaction,
                            onDelete = onDeleteTransaction,
                            onClick = onTransactionClick,
                            merchantRepository = merchantRepository
                        )
                    }
                    
                    // Spacing between groups
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastCard(projectedMonthlySpend: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estimated end-of-month spend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${String.format("%.2f", projectedMonthlySpend)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismiss: () -> Unit,
    filterState: com.instantledger.ui.main.FilterState,
    availableCategories: List<String>,
    allTransactions: List<Transaction>,
    onFilterStateChange: (com.instantledger.ui.main.FilterState) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    
    var localApprovalStatus by remember { mutableStateOf(filterState.approvalStatus) }
    var localSelectedCategories by remember { mutableStateOf(filterState.selectedCategories) }
    var localPaymentModes by remember { mutableStateOf(filterState.paymentModes) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Scrollable filter options (independent scroll)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Transactions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Divider()
                
                // Approval Status (Pending / Approved / All)
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.instantledger.ui.main.ApprovalStatusFilter.values().forEach { status ->
                        FilterChip(
                            selected = localApprovalStatus == status,
                            onClick = {
                                localApprovalStatus = if (localApprovalStatus == status) null else status
                            },
                            label = {
                                Text(
                                    if (status == com.instantledger.ui.main.ApprovalStatusFilter.PENDING) "Pending" else "Approved"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Payment Mode
                Text(
                    text = "Payment Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMode.values().forEach { mode ->
                        val isSelected = localPaymentModes.contains(mode)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                localPaymentModes = if (isSelected) {
                                    localPaymentModes - mode
                                } else {
                                    localPaymentModes + mode
                                }
                            },
                            label = {
                                Text(
                                    when (mode) {
                                        PaymentMode.CASH -> "Cash"
                                        PaymentMode.UPI -> "UPI"
                                        PaymentMode.CARD -> "Card"
                                        PaymentMode.BANK -> "Bank"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Category (multi-select)
                if (availableCategories.isNotEmpty()) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    availableCategories.forEach { category ->
                        val isSelected = localSelectedCategories.contains(category)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                localSelectedCategories = if (isSelected) {
                                    localSelectedCategories - category
                                } else {
                                    localSelectedCategories + category
                                }
                            },
                            label = { Text(category) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Divider()
            
            // Sticky bottom action row (always visible; not clipped by nav bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        localApprovalStatus = null
                        localSelectedCategories = emptySet()
                        localPaymentModes = emptySet()
                        onFilterStateChange(com.instantledger.ui.main.FilterState())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear all")
                }
                Button(
                    onClick = {
                        val newFilterState = com.instantledger.ui.main.FilterState(
                            approvalStatus = localApprovalStatus,
                            selectedCategories = localSelectedCategories,
                            paymentModes = localPaymentModes
                        )
                        onFilterStateChange(newFilterState)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply filters")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun PendingApprovalHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = "Needs your confirmation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Select a category to approve these transactions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PendingTransactionCard(
    transaction: Transaction,
    category1: String,
    category2: String,
    onCategory1: () -> Unit,
    onCategory2: () -> Unit,
    onIgnore: () -> Unit,
    merchantRepository: com.instantledger.data.repository.MerchantRepository? = null
) {
    val isDebit = transaction.transactionType == com.instantledger.data.model.TransactionType.DEBIT
    val amountColor = if (isDebit) {
        com.instantledger.ui.theme.TransactionColors.debit()
    } else {
        com.instantledger.ui.theme.TransactionColors.credit()
    }

    var displayMerchantName by remember { mutableStateOf<String?>(null) }
    
    // Resolve merchant display name with priority
    LaunchedEffect(transaction.id, transaction.merchant, transaction.merchantOverride) {
        displayMerchantName = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            when {
                // Priority 1: Transaction-level override
                !transaction.merchantOverride.isNullOrBlank() -> transaction.merchantOverride
                
                // Priority 2: Managed merchant name
                merchantRepository != null -> {
                    val managedMerchant = merchantRepository.getMerchantByOriginalName(transaction.merchant)
                    managedMerchant?.displayName ?: transaction.merchant.takeIf { 
                        it.isNotBlank() && it != "Unknown" 
                    } ?: "Unknown"
                }
                
                // Priority 3: Parsed SMS merchant or "Unknown"
                else -> transaction.merchant.takeIf { 
                    it.isNotBlank() && it != "Unknown" 
                } ?: "Unknown"
            }
        }
    }
    
    val finalDisplayName = displayMerchantName ?: transaction.merchant.takeIf { 
        it.isNotBlank() && it != "Unknown" 
    } ?: "Unknown"
    
    // Pending transactions have no category; use default "other" icon
    val displayText = finalDisplayName
    val neutralBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val defaultIconVector = CategoryIcons.getImageVector(CategoryIcons.DEFAULT_ICON_KEY)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = neutralBorderColor, // Neutral border for pending/uncategorized
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            // Slightly warm background to distinguish pending auto-captured items
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Icon + Source chip (left) | Amount (right) — same order as TransactionCard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = defaultIconVector,
                            contentDescription = "Uncategorized",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
                Text(
                    text = if (isDebit) {
                        "-₹${String.format("%.2f", transaction.amount)}"
                    } else {
                        "+₹${String.format("%.2f", transaction.amount)}"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Row 2: (No category for pending)
            // Row 3: Merchant
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (transaction.notes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Quick actions – Main, Work, Ignore (tinted circular icon buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TintedCircleIconButton(
                    onClick = onCategory1,
                    imageVector = Icons.Filled.Home,
                    contentDescription = category1,
                    modifier = Modifier.sizeIn(maxWidth = 48.dp)
                )
                TintedCircleIconButton(
                    onClick = onCategory2,
                    imageVector = Icons.Filled.Work,
                    contentDescription = category2,
                    modifier = Modifier.sizeIn(maxWidth = 48.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                TintedCircleIconButton(
                    onClick = onIgnore,
                    imageVector = Icons.Filled.Block,
                    contentDescription = "Ignore",
                    modifier = Modifier.sizeIn(maxWidth = 48.dp),
                    iconTint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun groupByDay(transactions: List<Transaction>): List<GroupedTransaction> {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
    
    return transactions
        .groupBy { transaction ->
            calendar.timeInMillis = transaction.timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            dateFormat.format(calendar.time)
        }
        .toList()
        .sortedByDescending { (_, transactions) ->
            transactions.firstOrNull()?.timestamp ?: 0L
        }
        .map { (header, trans) ->
            GroupedTransaction(header, trans.sortedByDescending { it.timestamp })
        }
}

private fun groupByWeek(transactions: List<Transaction>): List<GroupedTransaction> {
    val calendar = Calendar.getInstance()
    val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
    
    return transactions
        .groupBy { transaction ->
            calendar.timeInMillis = transaction.timestamp
            val month = monthFormat.format(calendar.time)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val weekNumber = ((dayOfMonth - 1) / 7) + 1
            
            val weekNames = listOf("1st", "2nd", "3rd", "4th", "5th")
            val weekName = if (weekNumber <= weekNames.size) {
                weekNames[weekNumber - 1]
            } else {
                "${weekNumber}th"
            }
            
            "$weekName week of $month"
        }
        .toList()
        .sortedByDescending { (_, transactions) ->
            transactions.firstOrNull()?.timestamp ?: 0L
        }
        .map { (header, trans) ->
            GroupedTransaction(header, trans.sortedByDescending { it.timestamp })
        }
}

private fun groupByMonth(transactions: List<Transaction>): List<GroupedTransaction> {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    return transactions
        .groupBy { transaction ->
            calendar.timeInMillis = transaction.timestamp
            dateFormat.format(calendar.time)
        }
        .toList()
        .sortedByDescending { (_, transactions) ->
            transactions.firstOrNull()?.timestamp ?: 0L
        }
        .map { (header, trans) ->
            GroupedTransaction(header, trans.sortedByDescending { it.timestamp })
        }
}
