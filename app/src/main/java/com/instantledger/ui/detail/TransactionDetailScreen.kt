package com.instantledger.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.instantledger.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: Transaction,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onApprove: ((Transaction) -> Unit)? = null,
    onBack: () -> Unit,
    merchantRepository: com.instantledger.data.repository.MerchantRepository? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share functionality - future enhancement */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Approve button (only for pending transactions with category)
                    if ((!transaction.isApproved || transaction.category.isNullOrBlank()) && onApprove != null) {
                        Button(
                            onClick = { 
                                if (transaction.category.isNullOrBlank()) {
                                    // If no category, navigate to edit instead
                                    onEdit(transaction)
                                } else {
                                    onApprove(transaction)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (transaction.category.isNullOrBlank()) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                            enabled = !transaction.category.isNullOrBlank() // Only enable if category exists
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (transaction.category.isNullOrBlank()) "Add Category" else "Approve")
                        }
                    }
                    
                    // Edit button
                    OutlinedButton(
                        onClick = { onEdit(transaction) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    
                    // Delete button
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Section - Large Amount Display
            HeroSection(transaction = transaction)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Information Grid
            InformationGrid(
                transaction = transaction,
                merchantRepository = merchantRepository,
                onEdit = onEdit
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Raw SMS Text (if auto-captured)
            if (transaction.sourceType == SourceType.SMS && transaction.rawTextHash != null) {
                RawSMSSection(rawTextHash = transaction.rawTextHash)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = {
                Text("Are you sure you want to delete this transaction? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(transaction)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroSection(transaction: Transaction) {
    val isDebit = transaction.transactionType == TransactionType.DEBIT
    val amountColor = if (isDebit) {
        com.instantledger.ui.theme.TransactionColors.debit()
    } else {
        com.instantledger.ui.theme.TransactionColors.credit()
    }
    
    val statusColor = if (transaction.isApproved) {
        com.instantledger.ui.theme.TransactionColors.credit()
    } else {
        MaterialTheme.colorScheme.primary // Primary color for pending
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Badge
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        if (transaction.isApproved) "Approved" else "Pending Approval",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = statusColor.copy(alpha = 0.2f),
                    labelColor = statusColor
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Large Amount Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isDebit) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "₹${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
        }
    }
}

@Composable
private fun InformationGrid(
    transaction: Transaction,
    merchantRepository: com.instantledger.data.repository.MerchantRepository? = null,
    onEdit: (Transaction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Time & Date
        InfoRow(
            icon = Icons.Default.Schedule,
            label = "Date & Time",
            value = formatTimestamp(transaction.timestamp)
        )
        
        Divider()
        
        // Source Type
        InfoRow(
            icon = when (transaction.entryType) {
                EntryType.AUTO_CAPTURED -> Icons.Default.Sms
                EntryType.USER_ENTERED -> Icons.Default.Edit
                EntryType.USER_MODIFIED -> Icons.Default.EditNote
            },
            label = "Source",
            value = when (transaction.entryType) {
                EntryType.AUTO_CAPTURED -> "Auto-Captured from SMS"
                EntryType.USER_ENTERED -> "Manually Entered"
                EntryType.USER_MODIFIED -> "Manually Modified"
            }
        )
        
        Divider()
        
        // Payment Mode
        InfoRow(
            icon = Icons.Default.Payment,
            label = "Payment Mode",
            value = when (transaction.paymentMode) {
                PaymentMode.CASH -> "💵 Cash"
                PaymentMode.UPI -> "📱 UPI"
                PaymentMode.CARD -> "💳 Card"
                PaymentMode.BANK -> "🏛️ Bank"
            }
        )
        
        Divider()
        
        // Merchant Name with display priority
        var displayMerchantName by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(transaction.id, transaction.merchant, transaction.merchantOverride, merchantRepository) {
            displayMerchantName = withContext(Dispatchers.IO) {
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
        
        val isMerchantUnknown = finalDisplayName == "Unknown"
        InfoRow(
            icon = Icons.Default.Store,
            label = "Merchant",
            value = if (isMerchantUnknown) {
                "Unknown (Tap to edit)"
            } else {
                finalDisplayName
            },
            highlight = isMerchantUnknown,
            onClick = if (isMerchantUnknown) { { onEdit(transaction) } } else null
        )
        
        // Show merchant override if present
        if (!transaction.merchantOverride.isNullOrBlank()) {
            Divider()
            InfoRow(
                icon = Icons.Default.Edit,
                label = "Custom Name",
                value = transaction.merchantOverride
            )
        }
        
        Divider()
        
        // Category
        InfoRow(
            icon = Icons.Default.Category,
            label = "Category",
            value = transaction.category ?: "Uncategorized"
        )
        
        Divider()
        
        // Transaction Type
        InfoRow(
            icon = if (transaction.transactionType == TransactionType.DEBIT) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
            label = "Type",
            value = if (transaction.transactionType == TransactionType.DEBIT) "Debit" else "Credit"
        )
        
        // Notes (if available)
        if (!transaction.notes.isNullOrBlank()) {
            Divider()
            InfoRow(
                icon = Icons.Default.Note,
                label = "Notes",
                value = transaction.notes
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    }
    
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                    color = if (highlight) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RawSMSSection(rawTextHash: String) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Raw Bank Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Hash: ${rawTextHash.take(32)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
                Text(
                    text = "Note: Original SMS text is stored as a hash for duplicate detection. Full text is not retained for privacy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    
    val dateFormat = SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault())
    return dateFormat.format(calendar.time)
}
