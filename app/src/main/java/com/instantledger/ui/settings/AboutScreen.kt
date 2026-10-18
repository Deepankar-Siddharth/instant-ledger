package com.instantledger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val packageManager = context.packageManager
    val packageInfo = try {
        packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
    val versionName = packageInfo?.versionName ?: "1.0"
    val versionCode = packageInfo?.longVersionCode ?: 1
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("About Instant Ledger") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Name and Version
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Instant Ledger",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version $versionName (Build $versionCode)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Divider()
            
            // Mission Statement
            Text(
                text = "Mission",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Empowering financial transparency through local-only automation.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Divider()
            
            // Security Badges
            Text(
                text = "Security & Privacy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            SecurityBadge(
                icon = Icons.Default.Lock,
                title = "SQLCipher Encryption",
                description = "Industry-standard database encryption protects your financial data"
            )
            
            SecurityBadge(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Lock",
                description = "Optional fingerprint or face authentication for app access"
            )
            
            SecurityBadge(
                icon = Icons.Default.CloudOff,
                title = "No Cloud Sync",
                description = "All data stays on your device—nothing is uploaded to external servers"
            )
            
            Divider()
            
            // Description
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Instant Ledger is a privacy-focused financial tracking app that automatically captures transactions from SMS messages sent by your bank. All processing happens locally on your device using encrypted storage—your financial data never leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Key Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            FeatureItem("Automatic SMS transaction capture")
            FeatureItem("Manual transaction entry (Cash, UPI, Card, Bank)")
            FeatureItem("Category management with icons and colors")
            FeatureItem("Merchant name management")
            FeatureItem("Time-based filtering (Today, Weekly, Monthly, Yearly)")
            FeatureItem("Transaction approval workflow")
            FeatureItem("Encrypted local database")
            FeatureItem("Export data to CSV")
            
            Divider()
            
            // Developer Info
            Text(
                text = "Developer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Instant Ledger Team",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SecurityBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
