package com.instantledger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.instantledger.data.preferences.EncryptedSettingsPreferences
import com.instantledger.security.BiometricManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricLockScreen(
    onBack: () -> Unit,
    settingsPreferences: EncryptedSettingsPreferences,
    modifier: Modifier = Modifier
) {
    // Handle back button
    BackHandler {
        onBack()
    }
    
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager(context) }
    
    var isEnabled by remember { mutableStateOf(settingsPreferences.isBiometricLockEnabled()) }
    var isBiometricAvailable by remember { mutableStateOf(biometricManager.isBiometricAvailable()) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Biometric Lock") },
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
            if (!isBiometricAvailable) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Biometric authentication is not available on this device. Please set up fingerprint or face unlock in your device settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biometric Lock",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Require fingerprint or face recognition to unlock the app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !isBiometricAvailable) {
                            // Can't enable if not available
                            return@Switch
                        }
                        isEnabled = enabled
                        settingsPreferences.setBiometricLockEnabled(enabled)
                    },
                    enabled = isBiometricAvailable
                )
            }
            
            Divider()
            
            Text(
                text = "When enabled, you'll need to authenticate with your fingerprint or face ID every time you open the app. This provides an extra layer of security for your financial data.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (isEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
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
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Biometric lock is active. The app will require authentication on startup.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
