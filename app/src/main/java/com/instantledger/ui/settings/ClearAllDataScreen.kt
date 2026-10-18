package com.instantledger.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.instantledger.data.database.AppDatabase
import com.instantledger.data.preferences.CategoryManager
import com.instantledger.data.preferences.EncryptedSettingsPreferences
import com.instantledger.security.EncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearAllDataScreen(
    onBack: () -> Unit,
    settingsPreferences: EncryptedSettingsPreferences,
    modifier: Modifier = Modifier
) {
    // Handle back button
    BackHandler {
        onBack()
    }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deletionMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Clear All Data") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Warning: This action cannot be undone",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "This will permanently delete:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Column(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• All transaction records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "• All categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "• All goals and savings data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "• All app settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            Text(
                text = "After deletion, the app will be reset to its initial state. You will need to set up categories and preferences again.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (deletionMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (deletionMessage!!.startsWith("Success"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = deletionMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = if (deletionMessage!!.startsWith("Success"))
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deleting...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Data")
                }
            }
        }
    }
    
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                Text("Are you absolutely sure you want to delete all data? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isDeleting = true
                        deletionMessage = null
                        
                        coroutineScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    try {
                                        // Delete all transactions
                                        val db = AppDatabase.getDatabase(context)
                                        db.transactionDao().deleteAllTransactions()
                                        db.goalDao().deleteAllGoals()
                                        db.unverifiedTransactionDao().deleteAllUnverifiedTransactions()
                                        
                                        // Delete categories
                                        CategoryManager(context).clearAllCategories()
                                        
                                        // Clear encrypted preferences (except biometric lock setting)
                                        val biometricEnabled = settingsPreferences.isBiometricLockEnabled()
                                        settingsPreferences.clearAll()
                                        if (biometricEnabled) {
                                            settingsPreferences.setBiometricLockEnabled(true)
                                        }
                                        
                                        // Regenerate encryption key
                                        EncryptionManager.regenerateKey(context)
                                    } catch (e: Exception) {
                                        throw e
                                    }
                                }
                                
                                deletionMessage = "Success! All data has been deleted. Please restart the app."
                            } catch (e: Exception) {
                                deletionMessage = "Error: ${e.message}"
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
