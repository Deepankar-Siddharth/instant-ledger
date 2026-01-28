package com.instantledger.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.instantledger.data.model.Transaction
import com.instantledger.data.repository.TransactionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataScreen(
    transactionRepository: TransactionRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                isExporting = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val transactions = transactionRepository.getAllTransactions().first()
                        val csvContent = generateCSV(transactions)
                        
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csvContent.toByteArray())
                        }
                        
                        exportMessage = "Export successful! ${transactions.size} transactions exported."
                    } catch (e: Exception) {
                        exportMessage = "Export failed: ${e.message}"
                    } finally {
                        isExporting = false
                    }
                }
            }
        } else {
            isExporting = false
            exportMessage = "Export cancelled"
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Export Data") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Export Your Transaction Data",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                text = "Export all your transactions to a CSV file that can be opened in Excel, Google Sheets, or any spreadsheet application.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TITLE, "instant_ledger_export_${System.currentTimeMillis()}.csv")
                    }
                    exportLauncher.launch(intent)
                },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporting...")
                } else {
                    Text("Export to CSV")
                }
            }
            
            if (exportMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (exportMessage!!.contains("success", ignoreCase = true)) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Text(
                        text = exportMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = if (exportMessage!!.contains("success", ignoreCase = true)) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }
    }
}

private fun generateCSV(transactions: List<Transaction>): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val csv = StringBuilder()
    
    // CSV Header
    csv.append("Date,Amount,Merchant,Category,Type,Payment Mode,Source,Notes\n")
    
    // CSV Rows
    transactions.forEach { transaction ->
        val date = dateFormat.format(Date(transaction.timestamp))
        val amount = transaction.amount
        val merchant = transaction.merchant.ifBlank { "Unknown" }
        val category = transaction.category ?: ""
        val type = transaction.transactionType.name
        val paymentMode = transaction.paymentMode.name
        val source = transaction.sourceType.name
        val notes = transaction.notes ?: ""
        
        // Escape commas and quotes in CSV
        fun escapeCSV(value: String): String {
            return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }
        
        csv.append("${escapeCSV(date)},${escapeCSV(amount.toString())},${escapeCSV(merchant)},${escapeCSV(category)},${escapeCSV(type)},${escapeCSV(paymentMode)},${escapeCSV(source)},${escapeCSV(notes)}\n")
    }
    
    return csv.toString()
}
