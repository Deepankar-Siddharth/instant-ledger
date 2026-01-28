package com.instantledger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.instantledger.data.model.Transaction
import com.instantledger.data.preferences.EncryptedSettingsPreferences
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.ui.dashboard.DashboardScreen
import com.instantledger.ui.detail.TransactionDetailScreen
import com.instantledger.ui.goals.GoalsScreen
import com.instantledger.ui.main.MainScreen
import com.instantledger.ui.main.MainViewModel
import com.instantledger.ui.main.TimePeriod
import com.instantledger.ui.manual.ManualEntryScreen
import com.instantledger.ui.settings.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.instantledger.ui.settings.ClearAllDataScreen
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.os.Build
import com.instantledger.data.model.TransactionType
import com.instantledger.data.model.PaymentMode
import com.instantledger.data.model.SourceType
import com.instantledger.data.model.EntryType

/**
 * Main navigation graph for the app
 * Uses a single NavController for all navigation
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    transactions: List<Transaction>,
    onAddTransaction: (com.instantledger.data.model.PaymentMode) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    transactionRepository: TransactionRepository?,
    settingsPreferences: EncryptedSettingsPreferences,
    viewModel: MainViewModel = hiltViewModel(),
    openPendingSection: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        // Dashboard (Main Screen)
        composable("dashboard") {
            MainScreen(
                transactions = transactions,
                onAddTransaction = onAddTransaction,
                onEditTransaction = onEditTransaction,
                onDeleteTransaction = onDeleteTransaction,
                transactionRepository = transactionRepository,
                settingsPreferences = settingsPreferences,
                viewModel = viewModel,
                navController = navController,
                openPendingSection = openPendingSection
            )
        }
        
        // Transaction Detail Screen
        composable("transaction_detail/{transactionId}") { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull()
            val transaction = transactions.find { it.id == transactionId }
            
            if (transaction != null) {
                TransactionDetailScreen(
                    transaction = transaction,
                    onEdit = { tx ->
                        navController.navigate("manual_entry/${tx.id}")
                    },
                    onDelete = { tx ->
                        onDeleteTransaction(tx)
                        navController.popBackStack()
                    },
                    onApprove = { tx ->
                        // Approve transaction - only if category is present
                        if (transactionRepository != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val updatedTx = tx.copy(
                                    isApproved = !tx.category.isNullOrBlank() // Only approve if category exists
                                )
                                transactionRepository.updateTransaction(updatedTx)
                            }
                        }
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    merchantRepository = viewModel.merchantRepository
                )
            } else {
                // Transaction not found - show error or go back
                navController.popBackStack()
            }
        }
        
        // Manual Entry Screen
        composable("manual_entry/{transactionId}") { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull()
            val transactionToEdit = transactions.find { it.id == transactionId }
            
            ManualEntryScreen(
                transactionToEdit = transactionToEdit,
                defaultPaymentMode = null,
                onSave = { _ ->
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("manual_entry") {
            ManualEntryScreen(
                transactionToEdit = null,
                defaultPaymentMode = null,
                onSave = { _ ->
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
        
        // Settings Hub
        composable("settings") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            SettingsHubScreen(
                onNavigateToSection = { section ->
                    when (section) {
                        SettingsSection.MANAGE_CATEGORIES -> navController.navigate("settings/categories")
                        SettingsSection.MANAGE_MERCHANTS -> navController.navigate("settings/merchants")
                        SettingsSection.BACKUP_RESTORE -> navController.navigate("settings/backup_restore")
                        SettingsSection.EXPORT_DATA -> navController.navigate("settings/export")
                        SettingsSection.BIOMETRIC_LOCK -> navController.navigate("settings/biometric")
                        SettingsSection.SMS_PARSING_RULES -> navController.navigate("settings/sms_rules")
                        SettingsSection.PRIVACY_POLICY -> navController.navigate("settings/privacy")
                        SettingsSection.GOALS -> navController.navigate("settings/goals")
                        SettingsSection.ABOUT -> navController.navigate("settings/about")
                        SettingsSection.CLEAR_ALL_DATA -> navController.navigate("settings/clear_data")
                        SettingsSection.HOW_IT_WORKS -> navController.navigate("settings/how_it_works")
                        else -> { /* Handle other cases */ }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                settingsPreferences = settingsPreferences
            )
        }

        // Settings Sub-screens
        composable("settings/categories") {
            ManageCategoriesScreen(
                categoryManager = androidx.compose.ui.platform.LocalContext.current.let { 
                    com.instantledger.data.preferences.CategoryManager(it) 
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings/merchants") {
            val merchantRepository = viewModel.merchantRepository
            if (merchantRepository != null && transactionRepository != null) {
                com.instantledger.ui.settings.ManageMerchantsScreen(
                    merchantRepository = merchantRepository,
                    transactionRepository = transactionRepository,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable("settings/backup_restore") {
            com.instantledger.ui.settings.BackupRestoreScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings/export") {
            if (transactionRepository != null) {
                ExportDataScreen(
                    transactionRepository = transactionRepository,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                PlaceholderSettingsScreen(
                    title = "Export Data",
                    description = "Transaction repository not available. Please restart the app.",
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable("settings/biometric") {
            BiometricLockScreen(
                onBack = {
                    navController.popBackStack()
                },
                settingsPreferences = settingsPreferences
            )
        }
        
        composable("settings/sms_rules") {
            SMSParsingRulesScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings/privacy") {
            PrivacyPolicyScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings/goals") {
            GoalsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings/about") {
            AboutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings/clear_data") {
            ClearAllDataScreen(
                onBack = {
                    navController.popBackStack()
                },
                settingsPreferences = settingsPreferences
            )
        }
        
        composable("settings/how_it_works") {
            HowItWorksScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
