package com.instantledger.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)

@Entity(tableName = "trip_participants")
data class TripParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "trip_id", index = true)
    val tripId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "contact")
    val contact: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trip_shares")
data class TripShareEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "trip_id", index = true)
    val tripId: Long,

    @ColumnInfo(name = "transaction_id", index = true)
    val transactionId: Long,

    @ColumnInfo(name = "participant_id", index = true)
    val participantId: Long,

    @ColumnInfo(name = "share_amount")
    val shareAmount: Double
)

