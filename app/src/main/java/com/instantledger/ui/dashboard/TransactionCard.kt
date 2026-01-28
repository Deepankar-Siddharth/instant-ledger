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
    
    // Category icon/emoji and color (derived ONLY from category, never merchant)
    val metadata = categoryMetadata // Local variable for smart casting
    val categoryIcon = metadata?.getDisplayIcon() ?: "📦" // Neutral placeholder if no category
    val categoryIsEmoji = metadata?.isEmoji ?: true
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon/Emoji (ALWAYS shown if category exists, derived ONLY from category)
            // Show neutral placeholder if no category
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = categoryColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (categoryIsEmoji) {
                    Text(
                        text = categoryIcon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                } else {
                    // For Material icons, show emoji representation
                    // In a full implementation, you'd map icon names to Icon vectors
                    Text(
                        text = categoryIcon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Refined AssistChip with lower opacity
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

                    // Group trip indicator
                    if (transaction.tripId != null) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = "👥 Group",
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
                    
                    // Category name (text-only, no visual control)
                    if (transaction.category != null) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Merchant name (text-only, always shown)
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
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Dynamic Typography: Green for Credit, Red for Debit, extra bold for visibility
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
                    style = MaterialTheme.typography.headlineMedium, // Increased from headlineSmall
                    fontWeight = FontWeight.ExtraBold, // Extra bold for maximum visibility
                    color = amountColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { onEdit(transaction) }) {
                        Text("Edit")
                    }
                    TextButton(onClick = { onDelete(transaction) }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
