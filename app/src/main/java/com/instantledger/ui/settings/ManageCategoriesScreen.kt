package com.instantledger.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.instantledger.data.preferences.CategoryManager
import com.instantledger.data.preferences.CategoryMetadata

private fun getEmojiForIconName(iconName: String): String {
    return when (iconName.lowercase()) {
        "home" -> "🏠"
        "work" -> "💼"
        "shoppingcart" -> "🛒"
        "restaurant" -> "🍔"
        "localgasstation" -> "⛽"
        "directionscar" -> "🚗"
        "localhospital" -> "🏥"
        "school" -> "🏫"
        "sportsesports" -> "🎮"
        "flight" -> "✈️"
        "hotel" -> "🏨"
        "attachmoney" -> "💰"
        "accountbalance" -> "🏦"
        "creditcard" -> "💳"
        "savings" -> "💵"
        "receipt" -> "🧾"
        else -> "📦"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    categoryManager: CategoryManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categoriesWithMetadata by remember { 
        mutableStateOf(categoryManager.getAllCategoriesWithMetadata()) 
    }
    var showAddEditScreen by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryMetadata?>(null) }
    
    // Refresh categories when screen is shown
    LaunchedEffect(Unit) {
        categoriesWithMetadata = categoryManager.getAllCategoriesWithMetadata()
    }
    
    BackHandler {
        if (showAddEditScreen) {
            showAddEditScreen = false
            editingCategory = null
        } else {
            onBack()
        }
    }
    
    if (showAddEditScreen || editingCategory != null) {
        AddEditCategoryScreen(
            categoryMetadata = editingCategory,
            onSave = { metadata ->
                if (editingCategory != null) {
                    // Update existing category
                    val oldName = editingCategory!!.name
                    if (oldName != metadata.name) {
                        // Name changed - update category name
                        categoryManager.updateCategory(oldName, metadata.name)
                    }
                    categoryManager.updateCategoryMetadata(oldName, metadata)
                } else {
                    // Add new category
                    categoryManager.addCategory(metadata.name)
                    categoryManager.saveCategoryMetadata(metadata)
                }
                categoriesWithMetadata = categoryManager.getAllCategoriesWithMetadata()
                showAddEditScreen = false
                editingCategory = null
            },
            onCancel = {
                showAddEditScreen = false
                editingCategory = null
            }
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showAddEditScreen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Category"
                        )
                    }
                }
            )
            
            if (categoriesWithMetadata.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "No categories yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Button(onClick = { showAddEditScreen = true }) {
                            Text("Add First Category")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddEditScreen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Category")
                        }
                    }
                    
                    items(categoriesWithMetadata) { categoryMetadata ->
                        CategoryItem(
                            categoryMetadata = categoryMetadata,
                            onEdit = {
                                editingCategory = categoryMetadata
                            },
                            onDelete = {
                                categoryManager.removeCategory(categoryMetadata.name)
                                categoryManager.deleteCategoryMetadata(categoryMetadata.name)
                                categoriesWithMetadata = categoryManager.getAllCategoriesWithMetadata()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    categoryMetadata: CategoryMetadata,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayIcon = categoryMetadata.getDisplayIcon()
    val categoryColor = categoryMetadata.color?.let { Color(it) } 
        ?: MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon/Emoji Display
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = categoryColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (categoryMetadata.isEmoji) {
                        Text(
                            text = displayIcon,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    } else {
                        // For Material icons, show emoji representation
                        // The actual icon is stored and will be used in transaction cards
                        val iconEmoji = getEmojiForIconName(categoryMetadata.icon ?: "")
                        Text(
                            text = iconEmoji,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                
                Column {
                    Text(
                        text = categoryMetadata.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Color Indicator
                if (categoryMetadata.color != null) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = categoryColor,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}
