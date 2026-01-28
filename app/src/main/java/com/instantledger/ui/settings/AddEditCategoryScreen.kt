package com.instantledger.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.instantledger.data.preferences.CategoryMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    categoryMetadata: CategoryMetadata? = null,
    onSave: (CategoryMetadata) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = categoryMetadata != null
    
    var categoryName by remember { mutableStateOf(categoryMetadata?.name ?: "") }
    var selectedIcon by remember { mutableStateOf<String?>(categoryMetadata?.icon) }
    var isEmojiSelected by remember { mutableStateOf(categoryMetadata?.isEmoji ?: false) }
    var selectedColor by remember { mutableStateOf<Long?>(categoryMetadata?.color) }
    var showEmojiInput by remember { mutableStateOf(false) }
    var emojiInput by remember { mutableStateOf("") }
    
    // Predefined Material icons (finance-safe)
    val materialIcons = listOf(
        "Home" to Icons.Default.Home,
        "Work" to Icons.Default.Work,
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "Restaurant" to Icons.Default.Restaurant,
        "LocalGasStation" to Icons.Default.LocalGasStation,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "LocalHospital" to Icons.Default.LocalHospital,
        "School" to Icons.Default.School,
        "SportsEsports" to Icons.Default.SportsEsports,
        "Flight" to Icons.Default.Flight,
        "Hotel" to Icons.Default.Hotel,
        "AttachMoney" to Icons.Default.AttachMoney,
        "AccountBalance" to Icons.Default.AccountBalance,
        "CreditCard" to Icons.Default.CreditCard,
        "Savings" to Icons.Default.Savings,
        "Receipt" to Icons.Default.Receipt
    )
    
    // Material 3 soft colors
    val materialColors = listOf(
        Color(0xFF6750A4) to "Purple",
        Color(0xFF625B71) to "Purple Grey",
        Color(0xFF7D5260) to "Pink",
        Color(0xFFB3261E) to "Red",
        Color(0xFF7D2E00) to "Deep Orange",
        Color(0xFF7C4A00) to "Orange",
        Color(0xFF5D4037) to "Brown",
        Color(0xFF455A64) to "Blue Grey",
        Color(0xFF1C4B82) to "Blue",
        Color(0xFF006874) to "Cyan",
        Color(0xFF00695C) to "Teal",
        Color(0xFF2D5016) to "Green",
        Color(0xFF5C9210) to "Light Green",
        Color(0xFF8C5000) to "Amber"
    )
    
    val isValid = categoryName.isNotBlank() && selectedIcon != null
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(if (isEditing) "Edit Category" else "Add Category") },
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Category Name Input
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                placeholder = { Text("e.g. Main, Work, Personal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = categoryName.isBlank()
            )
            
            if (categoryName.isBlank()) {
                Text(
                    text = "Category name is required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            
            Divider()
            
            // Icon Selection Section
            Text(
                text = "Category Icon",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Material Icons Grid
            Text(
                text = "Icons",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(materialIcons) { (iconName, iconVector) ->
                    val isSelected = !isEmojiSelected && selectedIcon == iconName
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedIcon = iconName
                                isEmojiSelected = false
                                showEmojiInput = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = iconName,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Emoji Option
            OutlinedButton(
                onClick = { showEmojiInput = !showEmojiInput },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Custom Emoji")
            }
            
            if (showEmojiInput) {
                OutlinedTextField(
                    value = emojiInput,
                    onValueChange = { 
                        emojiInput = it.take(1) // Only allow single character
                        if (emojiInput.isNotBlank()) {
                            selectedIcon = emojiInput
                            isEmojiSelected = true
                        }
                    },
                    label = { Text("Emoji") },
                    placeholder = { Text("Enter a single emoji") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "Enter a single emoji character",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
                
                if (isEmojiSelected && selectedIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedIcon!!,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
            
            Divider()
            
            // Color Selection (Optional)
            Text(
                text = "Category Color (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Used as border/accent color on transaction cards",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(120.dp)
            ) {
                items(materialColors) { (color, name) ->
                    val colorLong = color.value.toLong()
                    val isSelected = selectedColor == colorLong
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                color = color,
                                shape = CircleShape
                            )
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColor = if (isSelected) null else colorLong
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // "No Color" option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedColor = null }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Text(
                    text = "No color (use default)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Divider()
            
            // Live Preview
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            TransactionCardPreview(
                categoryName = categoryName.ifBlank { "Category Name" },
                icon = selectedIcon ?: "📦",
                isEmoji = isEmojiSelected,
                color = selectedColor?.let { Color(it) }
            )
            
            if (isEditing) {
                Text(
                    text = "Changes will apply to all transactions using this category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val metadata = CategoryMetadata(
                            name = categoryName.trim(),
                            icon = selectedIcon,
                            isEmoji = isEmojiSelected,
                            color = selectedColor
                        )
                        onSave(metadata)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isValid
                ) {
                    Text(if (isEditing) "Update Category" else "Save Category")
                }
            }
        }
    }
}

@Composable
private fun TransactionCardPreview(
    categoryName: String,
    icon: String,
    isEmoji: Boolean,
    color: Color?
) {
    val previewColor = color ?: MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = previewColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = previewColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isEmoji) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                } else {
                    // For preview, show a generic icon
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = previewColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sample Merchant",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-₹100.00",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
