package com.instantledger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SMSParsingRulesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("SMS Parsing Rules") },
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
                text = "Bank Keywords Being Tracked",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                text = "Instant Ledger automatically detects transactions from SMS messages sent by banks. The app recognizes patterns from major Indian banks.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Divider()
            
            SectionTitle("Supported Banks")
            BankCard("State Bank of India (SBI)", listOf("SBI", "State Bank"))
            BankCard("HDFC Bank", listOf("HDFC"))
            BankCard("ICICI Bank", listOf("ICICI"))
            BankCard("Axis Bank", listOf("AXIS"))
            BankCard("Kotak Mahindra Bank", listOf("KOTAK"))
            BankCard("Punjab National Bank (PNB)", listOf("PNB"))
            BankCard("Bank of India (BOI)", listOf("BOI"))
            
            Divider()
            
            SectionTitle("Amount Detection Patterns")
            PatternCard(
                "Currency Symbols",
                listOf("Rs.", "INR", "₹")
            )
            PatternCard(
                "Amount Formats",
                listOf("Rs. 1,234.56", "INR 1234.56", "₹1,234.56", "1234.56 Rs.")
            )
            
            Divider()
            
            SectionTitle("Transaction Type Keywords")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Debit Keywords",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("debited", "spent", "paid", "withdrawn", "deducted", "charged").forEach { keyword ->
                        Text(
                            text = "• $keyword",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Credit Keywords",
                        style = MaterialTheme.typography.titleSmall,
                        color = com.instantledger.ui.theme.TransactionColors.credit()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("credited", "received", "deposited", "refunded", "reversed").forEach { keyword ->
                        Text(
                            text = "• $keyword",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                        )
                    }
                }
            }
            
            Divider()
            
            SectionTitle("Payment Mode Detection")
            PatternCard(
                "UPI",
                listOf("upi", "gpay", "phonepe", "paytm", "bhim")
            )
            PatternCard(
                "Card",
                listOf("card", "visa", "mastercard", "rupay", "debit card", "credit card")
            )
            PatternCard(
                "Bank Transfer",
                listOf("neft", "imps", "rtgs", "transfer", "bank")
            )
            
            Divider()
            
            SectionTitle("Account Number Patterns")
            Text(
                text = "The app detects account numbers using patterns like:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• A/c *1234",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = "• Account 1234",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = "• Savings/Current Account",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 8.dp)
            )
            
            Divider()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Note: Parsing rules are automatically applied. The app uses regex patterns to extract transaction details from SMS messages. If a transaction is not detected, you can manually add it using the '+' button.",
                    style = MaterialTheme.typography.bodySmall,
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
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun BankCard(bankName: String, keywords: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = bankName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keywords: ${keywords.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PatternCard(title: String, patterns: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            patterns.forEach { pattern ->
                Text(
                    text = "• $pattern",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
