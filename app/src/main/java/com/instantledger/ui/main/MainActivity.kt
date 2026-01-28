package com.instantledger.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.instantledger.data.model.Transaction
import com.instantledger.data.preferences.EncryptedSettingsPreferences
import com.instantledger.security.BiometricManager
import com.instantledger.security.PinLockManager
import com.instantledger.ui.navigation.AppNavHost
import com.instantledger.ui.security.PinEntryScreen
import com.instantledger.ui.security.EncryptionErrorScreen
import com.instantledger.ui.theme.InstantLedgerTheme
import com.instantledger.security.EncryptionException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var settingsPreferences: EncryptedSettingsPreferences
    
    @Inject
    lateinit var pinLockManager: PinLockManager
    
    private lateinit var biometricManager: BiometricManager
    
    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val receiveGranted = results[Manifest.permission.RECEIVE_SMS] ?: false
        val readGranted = results[Manifest.permission.READ_SMS] ?: false
        if (!receiveGranted || !readGranted) {
            android.util.Log.w("InstantLedger", "MainActivity: SMS permissions not fully granted (RECEIVE_SMS=$receiveGranted, READ_SMS=$readGranted)")
        } else {
            android.util.Log.d("InstantLedger", "MainActivity: SMS permissions granted")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inject dependencies (Hilt will handle this)
        biometricManager = BiometricManager(this)
        
        checkPermissions()
        
        setContent {
            InstantLedgerTheme {
                val navController = rememberNavController()
                val transactions by viewModel.transactions.collectAsState()
                val context = LocalContext.current
                var isAuthenticated by remember { mutableStateOf(!settingsPreferences.isBiometricLockEnabled() && !pinLockManager.isPinSet()) }
                var showPinEntry by remember { mutableStateOf(false) }
                var pinEntryMode by remember { mutableStateOf<PinEntryMode?>(null) }
                var encryptionException by remember { mutableStateOf<EncryptionException?>(null) }
                
                // Check for encryption errors on startup
                LaunchedEffect(Unit) {
                    try {
                        // Try to access database to trigger initialization
                        com.instantledger.data.database.AppDatabase.getDatabase(context)
                    } catch (e: EncryptionException) {
                        encryptionException = e
                        android.util.Log.e("InstantLedger", "MainActivity: Encryption error detected", e)
                    } catch (e: Exception) {
                        // Check if it's an EncryptionException wrapped in another exception
                        val cause = e.cause
                        if (cause is EncryptionException) {
                            encryptionException = cause
                        } else {
                            android.util.Log.e("InstantLedger", "MainActivity: Unexpected error", e)
                        }
                    }
                }
                
                // Auto-trigger authentication on launch if enabled
                LaunchedEffect(Unit) {
                    val needsAuth = settingsPreferences.isBiometricLockEnabled() || pinLockManager.isPinSet()
                    if (needsAuth && !isAuthenticated) {
                        if (biometricManager.isBiometricAvailable() && settingsPreferences.isBiometricLockEnabled()) {
                            // Try biometric first
                            @Suppress("UNCHECKED_CAST")
                            biometricManager.authenticate(
                                activity = this@MainActivity as androidx.fragment.app.FragmentActivity,
                                onSuccess = { isAuthenticated = true },
                                onError = { _ ->
                                    // Biometric failed, show PIN if available
                                    if (pinLockManager.isPinSet()) {
                                        showPinEntry = true
                                        pinEntryMode = PinEntryMode.VERIFY
                                    }
                                },
                                onCancel = {
                                    // User cancelled, show PIN if available
                                    if (pinLockManager.isPinSet()) {
                                        showPinEntry = true
                                        pinEntryMode = PinEntryMode.VERIFY
                                    }
                                }
                            )
                        } else if (pinLockManager.isPinSet()) {
                            // Only PIN available
                            showPinEntry = true
                            pinEntryMode = PinEntryMode.VERIFY
                        } else {
                            // No authentication required
                            isAuthenticated = true
                        }
                    }
                }
                
                // Show PIN entry screen
                if (showPinEntry && pinEntryMode != null) {
                    PinEntryScreen(
                        title = if (pinEntryMode == PinEntryMode.VERIFY) "Enter PIN" else "Set PIN",
                        subtitle = if (pinEntryMode == PinEntryMode.VERIFY) "Enter your PIN to unlock" else "Create a 4-6 digit PIN",
                        onPinEntered = { pin ->
                            if (pinEntryMode == PinEntryMode.VERIFY) {
                                if (pinLockManager.verifyPin(pin)) {
                                    isAuthenticated = true
                                    showPinEntry = false
                                } else {
                                    // Show error - PIN incorrect
                                }
                            } else {
                                // Setting PIN
                                if (pinLockManager.setPin(pin)) {
                                    isAuthenticated = true
                                    showPinEntry = false
                                }
                            }
                        },
                        onCancel = {
                            if (pinEntryMode == PinEntryMode.VERIFY) {
                                // Can't cancel PIN verification
                            } else {
                                showPinEntry = false
                                pinEntryMode = null
                            }
                        }
                    )
                } else if (!isAuthenticated && (settingsPreferences.isBiometricLockEnabled() || pinLockManager.isPinSet())) {
                    // Show locked screen
                    LockedScreen(
                        onAuthenticate = {
                            if (biometricManager.isBiometricAvailable() && settingsPreferences.isBiometricLockEnabled()) {
                                @Suppress("UNCHECKED_CAST")
                                biometricManager.authenticate(
                                    activity = this@MainActivity as androidx.fragment.app.FragmentActivity,
                                    onSuccess = { isAuthenticated = true },
                                    onError = { _ ->
                                        // Biometric failed, show PIN
                                        if (pinLockManager.isPinSet()) {
                                            showPinEntry = true
                                            pinEntryMode = PinEntryMode.VERIFY
                                        }
                                    },
                                    onCancel = {
                                        // Show PIN fallback
                                        if (pinLockManager.isPinSet()) {
                                            showPinEntry = true
                                            pinEntryMode = PinEntryMode.VERIFY
                                        }
                                    }
                                )
                            } else if (pinLockManager.isPinSet()) {
                                showPinEntry = true
                                pinEntryMode = PinEntryMode.VERIFY
                            } else {
                                isAuthenticated = true
                            }
                        }
                    )
                } else if (encryptionException != null) {
                    // Show blocking encryption error screen
                    EncryptionErrorScreen(
                        exception = encryptionException!!,
                        onRetry = {
                            encryptionException = null
                            // Retry by attempting to reinitialize database
                            try {
                                com.instantledger.data.database.AppDatabase.getDatabase(context)
                            } catch (e: EncryptionException) {
                                encryptionException = e
                            } catch (e: Exception) {
                                val cause = e.cause
                                if (cause is EncryptionException) {
                                    encryptionException = cause
                                }
                            }
                        },
                        onResetApp = {
                            // Clear all data and reset app
                            try {
                                // Delete database file
                                context.deleteDatabase("instant_ledger.db")
                                // Clear encrypted preferences
                                com.instantledger.security.EncryptionManager.regenerateKey(context)
                                // Reset exception state
                                encryptionException = null
                                // Restart app
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            } catch (e: Exception) {
                                android.util.Log.e("InstantLedger", "MainActivity: Failed to reset app", e)
                            }
                        }
                    )
                } else {
                    // Check if we should open to pending section (from notification)
                    val openPending = intent?.getBooleanExtra(
                        com.instantledger.notification.TransactionNotificationManager.EXTRA_OPEN_PENDING,
                        false
                    ) ?: false
                    
                    AppNavHost(
                        navController = navController,
                        transactions = transactions,
                        onAddTransaction = { _ ->
                            navController.navigate("manual_entry")
                        },
                        onEditTransaction = { transaction ->
                            navController.navigate("manual_entry/${transaction.id}")
                        },
                        onDeleteTransaction = { transaction ->
                            viewModel.deleteTransaction(transaction)
                        },
                        transactionRepository = viewModel.transactionRepository,
                        settingsPreferences = settingsPreferences,
                        viewModel = viewModel,
                        openPendingSection = openPending
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-authenticate when app comes to foreground if biometric lock is enabled
        if (settingsPreferences.isBiometricLockEnabled()) {
            // This will trigger re-authentication in the composable
        }
    }
    
    private fun checkPermissions() {
        // Request SMS permissions so auto-capture can work in real time.
        // In production, this could be moved behind a toggle when user enables auto-capture.
        val receiveGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!receiveGranted || !readGranted) {
            requestSmsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
        } else {
            android.util.Log.d("InstantLedger", "MainActivity: SMS permissions already granted")
        }
    }
    
    
    enum class PinEntryMode {
        VERIFY, SET
    }
}
