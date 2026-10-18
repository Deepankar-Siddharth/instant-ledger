package com.instantledger.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey
    @ColumnInfo(name = "original_name")
    val originalName: String, // The parsed/raw merchant name from SMS
    
    @ColumnInfo(name = "display_name")
    val displayName: String, // User-defined friendly name
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
