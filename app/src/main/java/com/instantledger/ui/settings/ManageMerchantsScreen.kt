package com.instantledger.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.instantledger.data.database.entities.MerchantEntity
import com.instantledger.data.repository.MerchantRepository
import com.instantledger.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMerchantsScreen(
    merchantRepository: MerchantRepository,
    transactionRepository: TransactionRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val merchants by merchantRepository.getAllMerchants().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMerchant by remember { mutableStateOf<MerchantEntity?>(null) }
    var newOriginalName by remember { mutableStateOf("") }
    var newDisplayName by remember { mutableStateOf("") }
    var originalNameError by remember { mutableStateOf<String?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) }
    var usageCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    // Load usage counts
    LaunchedEffect(merchants) {
        scope.launch(Dispatchers.IO) {
            val counts = merchants.associateWith { merchant ->
                transactionRepository.getAllTransactions().first().count { 
                    it.merchant == merchant.originalName || it.merchantOverride == merchant.originalName
                }
            }
            usageCounts = counts.mapKeys { it.key.originalName }
        }
    }
    
    BackHandler {
        onBack()
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Manage Merchants") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { 
                    newOriginalName = ""
                    newDisplayName = ""
                    editingMerchant = null
                    showAddDialog = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Merchant")
                }
            }
        )
        
        if (merchants.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No merchants yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Button(onClick = { showAddDialog = true }) {
                        Text("Add First Merchant")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(merchants) { merchant ->
                    MerchantItem(
                        merchant = merchant,
                        usageCount = usageCounts[merchant.originalName] ?: 0,
                        onEdit = {
                            editingMerchant = merchant
                            newOriginalName = merchant.originalName
                            newDisplayName = merchant.displayName
                            showAddDialog = true
                        },
                        onDelete = {
                            scope.launch(Dispatchers.IO) {
                                merchantRepository.deleteMerchant(merchant)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                editingMerchant = null
                newOriginalName = ""
                newDisplayName = ""
                originalNameError = null
                displayNameError = null
            },
            title = { Text(if (editingMerchant != null) "Edit Merchant" else "Add Merchant") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newOriginalName,
                        onValueChange = { 
                            newOriginalName = it
                            originalNameError = null // Clear error on change
                        },
                        label = { Text("Original Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editingMerchant == null, // Can't change original name when editing
                        placeholder = { Text("e.g., ZMT*ORDER") },
                        isError = originalNameError != null,
                        supportingText = originalNameError?.let { { Text(it) } }
                    )
                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { 
                            newDisplayName = it
                            displayNameError = null // Clear error on change
                        },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Zomato") },
                        isError = displayNameError != null,
                        supportingText = displayNameError?.let { { Text(it) } }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Validate and trim inputs
                        val trimmedOriginalName = newOriginalName.trim()
                        val trimmedDisplayName = newDisplayName.trim()
                        
                        // Clear previous errors
                        originalNameError = null
                        displayNameError = null
                        
                        // Validate
                        var isValid = true
                        if (trimmedOriginalName.isBlank()) {
                            originalNameError = "Original name is required"
                            isValid = false
                        }
                        if (trimmedDisplayName.isBlank()) {
                            displayNameError = "Display name is required"
                            isValid = false
                        }
                        
                        if (isValid) {
                            // Save merchant - launch coroutine and close dialog after save completes
                            scope.launch(Dispatchers.IO) {
                                try {
                                    if (editingMerchant != null) {
                                        // Update existing merchant - use REPLACE strategy via insertMerchant
                                        val updatedMerchant = MerchantEntity(
                                            originalName = editingMerchant!!.originalName,
                                            displayName = trimmedDisplayName,
                                            createdAt = editingMerchant!!.createdAt,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        merchantRepository.insertMerchant(updatedMerchant) // REPLACE on conflict
                                    } else {
                                        // Insert new merchant
                                        val newMerchant = MerchantEntity(
                                            originalName = trimmedOriginalName,
                                            displayName = trimmedDisplayName
                                        )
                                        merchantRepository.insertMerchant(newMerchant)
                                    }
                                    
                                    // Close dialog and reset state after successful save
                                    // The Flow will automatically update the UI when database changes
                                    withContext(Dispatchers.Main) {
                                        showAddDialog = false
                                        editingMerchant = null
                                        newOriginalName = ""
                                        newDisplayName = ""
                                        originalNameError = null
                                        displayNameError = null
                                    }
                                } catch (e: Exception) {
                                    // Handle error - show error message on main thread
                                    withContext(Dispatchers.Main) {
                                        displayNameError = "Failed to save: ${e.message}"
                                        android.util.Log.e("ManageMerchants", "Error saving merchant", e)
                                    }
                                }
                            }
                        }
                    },
                    enabled = newOriginalName.isNotBlank() && newDisplayName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    editingMerchant = null
                    newOriginalName = ""
                    newDisplayName = ""
                    originalNameError = null
                    displayNameError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MerchantItem(
    merchant: MerchantEntity,
    usageCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Ensure merchant name is always visible - use displayName, fallback to originalName, then "Unnamed Merchant"
    val merchantName = merchant.displayName.takeIf { it.isNotBlank() } 
        ?: merchant.originalName.takeIf { it.isNotBlank() } 
        ?: "Unnamed Merchant"
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Primary: Merchant Name (always visible, prominent)
                Text(
                    text = merchantName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Below: Transaction count (muted)
                Text(
                    text = "$usageCount transaction${if (usageCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Trailing: Edit and Delete icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Delete", 
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Merchant") },
            text = { Text("Are you sure you want to delete \"${merchant.displayName}\"? This will not affect existing transactions.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
