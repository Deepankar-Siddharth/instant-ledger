package com.instantledger.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.instantledger.data.model.EntryType
import com.instantledger.data.model.Transaction
import com.instantledger.data.repository.MerchantRepository
import com.instantledger.data.preferences.CategoryIcons
import com.instantledger.data.preferences.CategoryManager
import com.instantledger.data.preferences.CategoryMetadata
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TransactionCard(
    transaction: Transaction,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onClick: ((Transaction) -> Unit)? = null,
    merchantRepository: MerchantRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager(context) }
    
    var displayMerchantName by remember { mutableStateOf<String?>(null) }
    var categoryMetadata by remember { mutableStateOf<CategoryMetadata?>(null) }
    
    // Resolve merchant display name with priority (text-only, no visual control)
    LaunchedEffect(transaction.id, transaction.merchant, transaction.merchantOverride) {
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
    
    // Load category metadata for icon and color
    LaunchedEffect(transaction.category) {
        categoryMetadata = withContext(Dispatchers.IO) {
            if (transaction.category != null && transaction.category.isNotBlank()) {
                categoryManager.getCategoryMetadata(transaction.category)
            } else {
                null
            }
        }
    }
    
    val finalDisplayName = displayMerchantName ?: transaction.merchant.takeIf { 
        it.isNotBlank() && it != "Unknown" 
    } ?: "Unknown"
    val sourceLabel = when (transaction.entryType) {
        EntryType.AUTO_CAPTURED -> "Auto"
        EntryType.USER_ENTERED -> "Manual"
        EntryType.USER_MODIFIED -> "Edited"
    }
    
    val sourceColor = when (transaction.entryType) {
        EntryType.AUTO_CAPTURED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        EntryType.USER_ENTERED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        EntryType.USER_MODIFIED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    }
    
    // Category icon (predefined only) and color (derived ONLY from category, never merchant)
    val metadata = categoryMetadata
    val categoryIconKey = metadata?.getResolvedIconKey() ?: CategoryIcons.DEFAULT_ICON_KEY
    val categoryIconVector = CategoryIcons.getImageVector(categoryIconKey)
    val categoryColorValue = metadata?.color
    val categoryColor = categoryColorValue?.let { Color(it) } 
        ?: MaterialTheme.colorScheme.outlineVariant // Neutral color if no category
    
    // Border color: Use category color if available, otherwise neutral
    val borderColor = if (categoryColorValue != null) {
        categoryColor.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }
    
    // Display text: Always show merchant name (text-only, no visual control)
    val displayText = finalDisplayName
    
    // Glassmorphism Card with 0.5dp border and soft shadow
    // Border color comes from category, not merchant
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(transaction) }
                } else {
                    Modifier
                }
            )
            .border(
                width = 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: icon + content column
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Category icon (compact)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = categoryColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIconVector,
                        contentDescription = transaction.category,
                        modifier = Modifier.size(20.dp),
                        tint = categoryColor
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row 1: Source chip only (category moved below)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = sourceLabel,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = sourceColor,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                        if (transaction.tripId != null) {
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = "Group",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    // Row 2: Category label (secondary: small font, muted, optional icon)
                    if (transaction.category != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = categoryIconVector,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = categoryColor.copy(alpha = 0.8f)
                            )
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                    // Row 3: Merchant / description
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    // Row 4 (optional): Notes
                    if (transaction.notes != null) {
                        Text(
                            text = transaction.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            // Right: Amount + actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isDebit = transaction.transactionType == com.instantledger.data.model.TransactionType.DEBIT
                val amountColor = if (isDebit) {
                    com.instantledger.ui.theme.TransactionColors.debit()
                } else {
                    com.instantledger.ui.theme.TransactionColors.credit()
                }
                Text(
                    text = if (isDebit) {
                        "-₹${String.format("%.2f", transaction.amount)}"
                    } else {
                        "+₹${String.format("%.2f", transaction.amount)}"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { onEdit(transaction) }) {
                        Text("Edit", style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = { onDelete(transaction) }) {
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
