package com.instantledger.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinEntryScreen(
    title: String = "Enter PIN",
    subtitle: String = "Enter your 4-6 digit PIN",
    onPinEntered: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (index < pin.length)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Number pad
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rows 1-3
            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (col in 1..3) {
                        val number = row * 3 + col
                        NumberButton(
                            number = number.toString(),
                            onClick = {
                                if (pin.length < 6) {
                                    pin += number
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Row 4: 0, backspace, cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NumberButton(
                    number = "0",
                    onClick = {
                        if (pin.length < 6) {
                            pin += "0"
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace"
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
        }
        
        // Auto-submit when PIN is 4-6 digits
        LaunchedEffect(pin) {
            if (pin.length in 4..6) {
                onPinEntered(pin)
            }
        }
    }
}

@Composable
private fun NumberButton(
    number: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .height(64.dp),
        shape = CircleShape
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineSmall,
            fontSize = 24.sp
        )
    }
}
