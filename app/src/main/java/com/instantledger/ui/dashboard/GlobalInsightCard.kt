package com.instantledger.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.instantledger.ui.main.ProjectedSpendData

@Composable
fun GlobalInsightCard(
    projectedSpendData: ProjectedSpendData?,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Collapsed/Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = projectedSpendData?.title ?: "Summary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (projectedSpendData == null || !projectedSpendData.hasData) {
                        Text(
                            text = "Not enough data for an estimate yet",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        // Net total for the current filtered dataset
                        val net = projectedSpendData.total
                        val netColor = when {
                            net < 0 -> MaterialTheme.colorScheme.error
                            net > 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val netText = formatAmount(net)
                        Text(
                            text = netText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                    }
                }
                
                // Expansion Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Expanded Section
            if (isExpanded && projectedSpendData != null && projectedSpendData.hasData) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Strict debit/credit breakdown for the current filtered dataset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total spent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatAmount(projectedSpendData.debitTotal),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total received",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatAmount(projectedSpendData.creditTotal),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 10000000 -> "₹${String.format("%.2f", amount / 10000000)}Cr"
        amount >= 100000 -> "₹${String.format("%.2f", amount / 100000)}L"
        amount >= 1000 -> "₹${String.format("%.2f", amount / 1000)}K"
        else -> "₹${String.format("%.2f", amount)}"
    }
}
