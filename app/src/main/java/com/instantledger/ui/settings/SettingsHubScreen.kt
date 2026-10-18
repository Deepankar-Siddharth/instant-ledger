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
import androidx.compose.ui.platform.LocalContext
import com.instantledger.data.preferences.CategoryManager
import com.instantledger.data.preferences.EncryptedSettingsPreferences
import com.instantledger.worker.PendingReminderScheduler

enum class SettingsSection {
    MANAGE_CATEGORIES,
    MANAGE_MERCHANTS,
    BACKUP_RESTORE,
    EXPORT_DATA,
    CLEAR_ALL_DATA,
    BIOMETRIC_LOCK,
    PRIVACY_MODE,
    SMS_PARSING_RULES,
    GOALS,
    ABOUT,
    HOW_IT_WORKS,
    PRIVACY_POLICY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    onBack: () -> Unit,
    onNavigateToSection: (SettingsSection) -> Unit,
    settingsPreferences: EncryptedSettingsPreferences,
    modifier: Modifier = Modifier
) {
    var newTransactionNotificationsEnabled by remember {
        mutableStateOf(settingsPreferences.areNewTransactionNotificationsEnabled())
    }
    var reminderNotificationsEnabled by remember {
        mutableStateOf(settingsPreferences.areReminderNotificationsEnabled())
    }
    // Handle back button
    BackHandler {
        onBack()
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Data Management Section
            item {
                SectionHeader("Data Management")
            }
            item {
                SettingsItem(
                    title = "Manage Categories",
                    icon = Icons.Default.Category,
                    onClick = { onNavigateToSection(SettingsSection.MANAGE_CATEGORIES) }
                )
            }
            item {
                SettingsItem(
                    title = "Manage Merchants",
                    icon = Icons.Default.Store,
                    onClick = { onNavigateToSection(SettingsSection.MANAGE_MERCHANTS) }
                )
            }
            item {
                SettingsItem(
                    title = "Backup & Restore",
                    subtitle = "Password-protected backup of all data",
                    icon = Icons.Default.Backup,
                    onClick = { onNavigateToSection(SettingsSection.BACKUP_RESTORE) }
                )
            }
            item {
                SettingsItem(
                    title = "Export Data",
                    subtitle = "CSV/JSON export for Excel/Google Sheets",
                    icon = Icons.Default.FileDownload,
                    onClick = { onNavigateToSection(SettingsSection.EXPORT_DATA) }
                )
            }
            item {
                SettingsItem(
                    title = "Clear All Data",
                    subtitle = "Security/Reset option",
                    icon = Icons.Default.Delete,
                    onClick = { onNavigateToSection(SettingsSection.CLEAR_ALL_DATA) },
                    isDestructive = true
                )
            }
            
            // Security Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Security")
            }
            item {
                SettingsItem(
                    title = "Biometric Lock",
                    subtitle = "Fingerprint/FaceID on app start",
                    icon = Icons.Default.Fingerprint,
                    onClick = { onNavigateToSection(SettingsSection.BIOMETRIC_LOCK) }
                )
            }
            item {
                SettingsItem(
                    title = "Privacy Mode",
                    subtitle = "Masks amounts on home screen until tapped",
                    icon = Icons.Default.Visibility,
                    onClick = { onNavigateToSection(SettingsSection.PRIVACY_MODE) }
                )
            }

            // Automation Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Automation")
            }
            item {
                SettingsItem(
                    title = "SMS Parsing Rules",
                    subtitle = "View/Edit bank keywords being tracked",
                    icon = Icons.Default.Rule,
                    onClick = { onNavigateToSection(SettingsSection.SMS_PARSING_RULES) }
                )
            }
            
            // Notifications Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Notifications")
            }
            item {
                NotificationToggleItem(
                    title = "New Transaction Notifications",
                    subtitle = "Get notified when transactions are detected",
                    enabled = newTransactionNotificationsEnabled,
                    onToggle = { enabled ->
                        newTransactionNotificationsEnabled = enabled
                        settingsPreferences.setNewTransactionNotificationsEnabled(enabled)
                    }
                )
            }
            item {
                val context = LocalContext.current
                NotificationToggleItem(
                    title = "Pending Reminder Notifications",
                    subtitle = "Daily reminders for unapproved transactions",
                    enabled = reminderNotificationsEnabled,
                    onToggle = { enabled ->
                        reminderNotificationsEnabled = enabled
                        settingsPreferences.setReminderNotificationsEnabled(enabled)
                        // Schedule or cancel reminders based on toggle
                        if (enabled) {
                            PendingReminderScheduler.scheduleReminders(context)
                        } else {
                            PendingReminderScheduler.cancelReminders(context)
                        }
                    }
                )
            }
            // Debug / Tools Section (currently empty – overlay tools removed)
            
            // About & Support Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("About & Support")
            }
            item {
                SettingsItem(
                    title = "About Instant Ledger",
                    subtitle = "Version number, Developer info",
                    icon = Icons.Default.Info,
                    onClick = { onNavigateToSection(SettingsSection.ABOUT) }
                )
            }
            item {
                SettingsItem(
                    title = "How it Works",
                    subtitle = "Short tutorial on SMS tracking",
                    icon = Icons.Default.Help,
                    onClick = { onNavigateToSection(SettingsSection.HOW_IT_WORKS) }
                )
            }
            item {
                SettingsItem(
                    title = "Privacy Policy",
                    subtitle = "Crucial for Play Store compliance",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { onNavigateToSection(SettingsSection.PRIVACY_POLICY) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    subtitle: String? = null,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}
