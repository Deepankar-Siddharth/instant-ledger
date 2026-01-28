package com.instantledger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.instantledger.data.preferences.CategoryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    categoryManager: CategoryManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categories by remember { mutableStateOf(categoryManager.getCategories()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editCategoryText by remember { mutableStateOf("") }
    
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Manage Categories",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            item {
                // Add Category Button
                Button(
                    onClick = { showAddDialog = true },
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
            
            // Categories List
            if (categories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No categories. Add one to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(categories) { category ->
                    CategoryItem(
                        category = category,
                        onEdit = {
                            editingCategory = category
                            editCategoryText = category
                        },
                        onDelete = {
                            categoryManager.removeCategory(category)
                            categories = categoryManager.getCategories()
                        }
                    )
                }
            }
        }
    }
    
    // Add Category Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryText.isNotBlank()) {
                            categoryManager.addCategory(newCategoryText)
                            categories = categoryManager.getCategories()
                            newCategoryText = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newCategoryText.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Edit Category Dialog
    editingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Edit Category") },
            text = {
                OutlinedTextField(
                    value = editCategoryText,
                    onValueChange = { editCategoryText = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editCategoryText.isNotBlank()) {
                            categoryManager.updateCategory(category, editCategoryText)
                            categories = categoryManager.getCategories()
                            editingCategory = null
                            editCategoryText = ""
                        }
                    },
                    enabled = editCategoryText.isNotBlank()
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
