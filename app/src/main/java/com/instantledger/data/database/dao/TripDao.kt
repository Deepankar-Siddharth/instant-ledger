package com.instantledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.instantledger.data.database.entities.TripEntity
import com.instantledger.data.database.entities.TripParticipantEntity
import com.instantledger.data.database.entities.TripShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    // Trips
    @Query("SELECT * FROM trips WHERE is_active = 1 ORDER BY created_at DESC")
    fun getActiveTrips(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    // Participants
    @Query("SELECT * FROM trip_participants WHERE trip_id = :tripId ORDER BY name ASC")
    suspend fun getParticipantsForTrip(tripId: Long): List<TripParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<TripParticipantEntity>)

    // Shares
    @Query("SELECT * FROM trip_shares WHERE trip_id = :tripId")
    suspend fun getSharesForTrip(tripId: Long): List<TripShareEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<TripShareEntity>)
}

