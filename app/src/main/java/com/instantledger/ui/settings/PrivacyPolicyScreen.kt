package com.instantledger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Privacy Policy") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Last Updated: January 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Divider()
            
            SectionTitle("1. Data Processing")
            Text(
                text = "Instant Ledger processes all SMS data locally on your device. We do not upload, transmit, or share any financial data with external servers or third parties. All transaction information remains stored securely on your device using encrypted local storage.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            SectionTitle("2. SMS Access")
            Text(
                text = "The app requires SMS read permissions to automatically capture transaction notifications from your bank. We only process SMS messages that contain transaction-related keywords (such as 'debited', 'credited', 'balance', etc.). We do not read or store any other SMS messages.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            SectionTitle("3. Local Storage")
            Text(
                text = "All transaction data is stored locally on your device using AES-256 encryption. The encryption keys are managed by Android Keystore, ensuring that your data cannot be accessed by other apps or unauthorized parties.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            SectionTitle("4. Data Control")
            Text(
                text = "You have complete control over your data. You can:\n\n• View all stored transactions\n• Edit or delete individual transactions\n• Export your data to CSV/JSON format\n• Clear all data at any time via Settings > Clear All Data",
                style = MaterialTheme.typography.bodyMedium
            )
            
            SectionTitle("5. No Data Collection")
            Text(
                text = "We do not collect, track, or analyze any personal information. The app does not use analytics, crash reporting, or any third-party services that could access your data.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            SectionTitle("6. Permissions")
            Text(
                text = "The app requires the following permissions:\n\n• SMS Read: To automatically capture transaction notifications\n• Biometric Authentication: To secure app access (optional)\n\nAll permissions are used solely for the stated purposes and are not shared with any external services.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            SectionTitle("7. Your Rights")
            Text(
                text = "You have the right to:\n\n• Access all your stored transaction data\n• Export your data in a portable format\n• Delete all data at any time\n• Uninstall the app, which removes all local data\n\nSince all data is stored locally, uninstalling the app will permanently delete all transaction records.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            SectionTitle("8. Contact")
            Text(
                text = "If you have any questions about this Privacy Policy or how we handle your data, please contact us through the app's About section.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "This app is designed with privacy as a core principle. Your financial data never leaves your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}
