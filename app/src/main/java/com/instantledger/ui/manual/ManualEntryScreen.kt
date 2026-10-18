package com.instantledger.ui.manual

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.instantledger.data.model.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.instantledger.data.preferences.CategoryManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import com.instantledger.data.database.entities.MerchantEntity
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    transactionToEdit: Transaction?,
    onSave: (Transaction) -> Unit,
    onCancel: () -> Unit,
    defaultPaymentMode: PaymentMode? = null,
    viewModel: ManualEntryViewModel = hiltViewModel()
) {
    // Handle back button
    BackHandler {
        onCancel()
    }
    
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager(context) }
    val categories = remember { mutableStateOf(categoryManager.getCategories()) }
    val merchants by viewModel.getAllMerchants().collectAsState(initial = emptyList())
    
    val isEditing = transactionToEdit != null
    
    var amountText by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    
    // Merchant selection state
    // Determine initial selected merchant from transactionToEdit
    var selectedMerchantId by remember {
        mutableStateOf<String?>(null) // null = no selection, "OTHER" = Other selected, merchant originalName = merchant selected
    }
    var otherMerchantText by remember { mutableStateOf("") } // Text input when "Other" is selected
    var showMerchantDropdown by remember { mutableStateOf(false) }
    
    // Initialize merchant selection from existing transaction
    // Wait for merchants to load before initializing
    LaunchedEffect(transactionToEdit, merchants) {
        if (transactionToEdit != null && selectedMerchantId == null) {
            // If there's a merchantOverride, it means "Other" was used
            if (!transactionToEdit.merchantOverride.isNullOrBlank()) {
                selectedMerchantId = "OTHER"
                otherMerchantText = transactionToEdit.merchantOverride
            } else {
                // Check if merchant matches a managed merchant
                val matchingMerchant = merchants.find { it.originalName == transactionToEdit.merchant }
                if (matchingMerchant != null) {
                    selectedMerchantId = matchingMerchant.originalName
                } else if (transactionToEdit.merchant.isNotBlank() && transactionToEdit.merchant != "Unknown") {
                    // Merchant exists but not in managed list - treat as "Other"
                    selectedMerchantId = "OTHER"
                    otherMerchantText = transactionToEdit.merchant
                }
            }
        }
    }
    
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "") }
    var notesText by remember { mutableStateOf(transactionToEdit?.notes ?: "") }
    var selectedPaymentMode by remember {
        mutableStateOf(transactionToEdit?.paymentMode ?: defaultPaymentMode ?: PaymentMode.CASH)
    }
    var selectedTransactionType by remember {
        mutableStateOf(transactionToEdit?.transactionType ?: TransactionType.DEBIT)
    }
    var selectedTimestamp by remember {
        mutableStateOf(transactionToEdit?.timestamp ?: System.currentTimeMillis())
    }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Refresh categories when screen is shown
    LaunchedEffect(Unit) {
        categories.value = categoryManager.getCategories()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (isEditing) "Edit Transaction" else "Add Transaction",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }
        
        val parsedAmount = amountText.toDoubleOrNull()
        if (parsedAmount == null || parsedAmount <= 0.0) {
            Text(
                text = "Enter a valid amount to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Show selected date/time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        Text(
            text = "Date & Time: ${dateFormat.format(selectedTimestamp)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        
        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedTimestamp
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            // Preserve the time portion when changing date
                            val calendar = Calendar.getInstance().apply { 
                                timeInMillis = selectedTimestamp 
                            }
                            val hour = calendar.get(Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(Calendar.MINUTE)
                            
                            // Set new date with preserved time
                            calendar.timeInMillis = dateMillis
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            
                            selectedTimestamp = calendar.timeInMillis
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        
        // Time Picker Dialog
        if (showTimePicker) {
            val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
            val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
            val initialMinute = calendar.get(Calendar.MINUTE)
            
            val timePickerState = rememberTimePickerState(
                initialHour = initialHour,
                initialMinute = initialMinute
            )
            
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        // Preserve the date portion when changing time
                        val newCalendar = Calendar.getInstance().apply { 
                            timeInMillis = selectedTimestamp 
                        }
                        newCalendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        newCalendar.set(Calendar.MINUTE, timePickerState.minute)
                        newCalendar.set(Calendar.SECOND, 0)
                        newCalendar.set(Calendar.MILLISECOND, 0)
                        
                        selectedTimestamp = newCalendar.timeInMillis
                        showTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }
        
        // Date/Time picker buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Date")
            }
            
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Time")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Merchant Selection Dropdown
        Text(
            text = "Merchant",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = showMerchantDropdown,
            onExpandedChange = { showMerchantDropdown = !showMerchantDropdown },
            modifier = Modifier.fillMaxWidth()
        ) {
            val displayText = when (selectedMerchantId) {
                null -> ""
                "OTHER" -> "Other"
                else -> merchants.find { it.originalName == selectedMerchantId }?.displayName ?: ""
            }
            
            OutlinedTextField(
                value = displayText,
                onValueChange = { }, // Read-only
                readOnly = true,
                label = { Text("Select Merchant") },
                placeholder = { Text("Choose a merchant") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMerchantDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = showMerchantDropdown,
                onDismissRequest = { showMerchantDropdown = false }
            ) {
                // List all managed merchants
                merchants.forEach { merchant ->
                    DropdownMenuItem(
                        text = { Text(merchant.displayName) },
                        onClick = {
                            selectedMerchantId = merchant.originalName
                            otherMerchantText = "" // Clear "Other" text when selecting a merchant
                            showMerchantDropdown = false
                        }
                    )
                }
                
                // Divider before "Other" option
                if (merchants.isNotEmpty()) {
                    Divider()
                }
                
                // "Other" option
                DropdownMenuItem(
                    text = { Text("Other") },
                    onClick = {
                        selectedMerchantId = "OTHER"
                        showMerchantDropdown = false
                    }
                )
            }
        }
        
        // Show optional text input when "Other" is selected
        if (selectedMerchantId == "OTHER") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = otherMerchantText,
                onValueChange = { otherMerchantText = it },
                label = { Text("Merchant name (optional)") },
                placeholder = { Text("Enter merchant name for this transaction only") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(
                        text = "This applies only to this transaction",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category Dropdown
        Text(
            text = "Category",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = showCategoryDropdown,
            onExpandedChange = { showCategoryDropdown = !showCategoryDropdown },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = { },
                readOnly = true,
                label = { Text("Select Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                categories.value.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Payment Mode",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaymentMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedPaymentMode == mode,
                    onClick = { selectedPaymentMode = mode },
                    label = { Text(mode.name) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Transaction Type",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Enhanced Transaction Type Selection with color coding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionType.values().forEach { type ->
                val isSelected = selectedTransactionType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTransactionType = type },
                    label = { 
                        Text(
                            if (type == TransactionType.DEBIT) "− Debit" 
                            else "+ Credit"
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (type == TransactionType.DEBIT) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        selectedLabelColor = if (type == TransactionType.DEBIT) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        // Determine merchant and merchantOverride based on selection
                        val (merchant, merchantOverride) = when (selectedMerchantId) {
                            null -> Pair("Unknown", null) // No selection
                            "OTHER" -> {
                                // If "Other" is selected, use the text input as override
                                // Store a placeholder in merchant field
                                Pair("Unknown", otherMerchantText.takeIf { it.isNotBlank() })
                            }
                            else -> {
                                // Managed merchant selected - use its originalName
                                val selectedMerchant = merchants.find { it.originalName == selectedMerchantId }
                                Pair(selectedMerchant?.originalName ?: "Unknown", null)
                            }
                        }
                        
                        viewModel.saveTransaction(
                            amount = amount,
                            merchant = merchant,
                            merchantOverride = merchantOverride,
                            category = selectedCategory.takeIf { it.isNotBlank() },
                            paymentMode = selectedPaymentMode,
                            timestamp = selectedTimestamp,
                            notes = notesText.takeIf { it.isNotBlank() },
                            transactionType = selectedTransactionType,
                            existingTransaction = transactionToEdit
                        )
                        onSave(transactionToEdit ?: Transaction(
                            id = 0,
                            timestamp = selectedTimestamp,
                            amount = amount,
                            merchant = merchant,
                            merchantOverride = merchantOverride,
                            category = selectedCategory,
                            accountType = null,
                            transactionType = selectedTransactionType,
                            paymentMode = selectedPaymentMode,
                            rawTextHash = null,
                            sourceType = SourceType.MANUAL,
                            entryType = EntryType.USER_ENTERED,
                            confidenceScore = 1.0f,
                            isRecurring = false,
                            projectId = null,
                            notes = notesText,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = amountText.toDoubleOrNull() != null && amountText.toDoubleOrNull()!! > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTransactionType == TransactionType.DEBIT) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (selectedTransactionType == TransactionType.DEBIT) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            ) {
                Text(if (isEditing) "Update" else "Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Date") },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
