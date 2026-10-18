package com.instantledger.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.instantledger.backup.BackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordDialogMode by remember { mutableStateOf<PasswordDialogMode?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Backup launcher
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showPasswordDialog = true
                passwordDialogMode = PasswordDialogMode.Backup(uri)
            }
        }
    }
    
    // Restore launcher
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showPasswordDialog = true
                passwordDialogMode = PasswordDialogMode.Restore(uri)
            }
        }
    }
    
    BackHandler(enabled = !isProcessing) {
        onBack()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Backup & Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    Text(
                        "Create a password-protected backup of all your data (transactions, categories, merchants, settings). " +
                        "Backups can be restored on any device. Keep your backup password safe - it cannot be recovered.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Backup Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Create Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Export all your data to a secure backup file. Choose a strong password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    val dbSize = remember { backupManager.getDatabaseSize() }
                    Text(
                        "Database size: ${formatFileSize(dbSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_TITLE, "instant_ledger_backup_${System.currentTimeMillis()}.ledger")
                            }
                            backupLauncher.launch(intent)
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Backup File")
                    }
                }
            }
            
            // Restore Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Restore Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Import data from a backup file. This will replace all current data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        "⚠️ Warning: Restoring will overwrite all existing data. Make sure you have a current backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "*/*"))
                            }
                            restoreLauncher.launch(intent)
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select Backup File")
                    }
                }
            }
            
            // Processing Indicator
            if (isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            processingMessage.ifEmpty { "Processing..." },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    
    // Password Dialog
    if (showPasswordDialog && passwordDialogMode != null) {
        PasswordInputDialog(
            mode = passwordDialogMode!!,
            onDismiss = { 
                showPasswordDialog = false
                passwordDialogMode = null
            },
            onConfirm = { password ->
                showPasswordDialog = false
                isProcessing = true
                processingMessage = when (passwordDialogMode) {
                    is PasswordDialogMode.Backup -> "Creating backup..."
                    is PasswordDialogMode.Restore -> "Restoring data..."
                    else -> "Processing..."
                }
                
                scope.launch {
                    val result = when (val mode = passwordDialogMode) {
                        is PasswordDialogMode.Backup -> {
                            try {
                                context.contentResolver.openOutputStream(mode.uri)?.use { output ->
                                    backupManager.createBackup(password, output)
                                } ?: Result.failure(Exception("Failed to open output stream"))
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        }
                        is PasswordDialogMode.Restore -> {
                            try {
                                context.contentResolver.openInputStream(mode.uri)?.use { input ->
                                    backupManager.restoreBackup(password, input)
                                } ?: Result.failure(Exception("Failed to open input stream"))
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        }
                        else -> Result.failure(Exception("Invalid mode"))
                    }
                    
                    val wasRestore = passwordDialogMode is PasswordDialogMode.Restore
                    passwordDialogMode = null
                    isProcessing = false
                    
                    if (result.isSuccess) {
                        successMessage = if (wasRestore) {
                            "Data restored successfully! The app will restart."
                        } else {
                            "Backup created successfully!"
                        }
                        showSuccessDialog = true
                        
                        // If restore, restart app
                        if (wasRestore) {
                            // Restart app after a short delay
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                restartApp(context)
                            }, 2000)
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        errorMessage = "Failed: ${error?.message ?: "Unknown error"}"
                        showErrorDialog = true
                    }
                }
            }
        )
    }
    
    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                onBack()
            },
            title = { Text("Success") },
            text = { Text(successMessage) },
            confirmButton = {
                TextButton(onClick = { 
                    showSuccessDialog = false
                    onBack()
                }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun PasswordInputDialog(
    mode: PasswordDialogMode,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val isBackup = mode is PasswordDialogMode.Backup
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBackup) "Create Backup" else "Restore Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (isBackup) {
                        "Enter a password to encrypt your backup file. Remember this password - you'll need it to restore."
                    } else {
                        "Enter the password used to create this backup."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = null
                    },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = passwordError != null
                )
                
                if (isBackup) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it
                            passwordError = null
                        },
                        label = { Text("Confirm Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = passwordError != null
                    )
                }
                
                if (passwordError != null) {
                    Text(
                        passwordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        password.isBlank() -> {
                            passwordError = "Password cannot be empty"
                        }
                        password.length < 6 -> {
                            passwordError = "Password must be at least 6 characters"
                        }
                        isBackup && password != confirmPassword -> {
                            passwordError = "Passwords do not match"
                        }
                        else -> {
                            onConfirm(password)
                        }
                    }
                },
                enabled = password.isNotBlank() && (!isBackup || confirmPassword.isNotBlank())
            ) {
                Text(if (isBackup) "Create Backup" else "Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private sealed class PasswordDialogMode {
    data class Backup(val uri: Uri) : PasswordDialogMode()
    data class Restore(val uri: Uri) : PasswordDialogMode()
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes bytes"
    }
}

private fun restartApp(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    if (context is Activity) {
        context.finish()
    }
    Runtime.getRuntime().exit(0)
}
