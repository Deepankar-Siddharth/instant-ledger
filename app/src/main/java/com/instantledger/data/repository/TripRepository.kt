package com.instantledger.data.repository

import com.instantledger.data.database.dao.TripDao
import com.instantledger.data.database.entities.TripParticipantEntity
import com.instantledger.data.database.entities.TripShareEntity
import com.instantledger.data.model.Trip
import com.instantledger.data.model.TripParticipant
import com.instantledger.data.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao
){
    fun getActiveTrips(): Flow<List<Trip>> {
        return tripDao.getActiveTrips().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getParticipantsForTrip(tripId: Long): List<TripParticipant> {
        return tripDao.getParticipantsForTrip(tripId).map { it.toDomain() }
    }

    /**
     * Save equal split shares for the given transaction and participants.
     */
    suspend fun saveEqualSharesForTransaction(
        tripId: Long,
        transactionId: Long,
        totalAmount: Double,
        participantIds: List<Long>
    ) {
        if (participantIds.isEmpty()) return

        val perParticipantShare = totalAmount / participantIds.size.toDouble()
        val shares = participantIds.map { participantId ->
            TripShareEntity(
                tripId = tripId,
                transactionId = transactionId,
                participantId = participantId,
                shareAmount = perParticipantShare
            )
        }
        tripDao.insertShares(shares)
    }

    /**
     * Calculate total owed per participant for a given trip.
     */
    suspend fun getTripBalances(tripId: Long): Map<TripParticipant, Double> {
        val participants = tripDao.getParticipantsForTrip(tripId)
        if (participants.isEmpty()) return emptyMap()

        val shares = tripDao.getSharesForTrip(tripId)
        if (shares.isEmpty()) return emptyMap()

        val participantById = participants.associateBy { it.id }

        val totalsByParticipantId: MutableMap<Long, Double> = mutableMapOf()
        for (share in shares) {
            totalsByParticipantId[share.participantId] =
                (totalsByParticipantId[share.participantId] ?: 0.0) + share.shareAmount
        }

        return totalsByParticipantId.mapNotNull { (participantId, total) ->
            val participant = participantById[participantId]?.toDomain()
            if (participant != null) {
                participant to total
            } else {
                null
            }
        }.toMap()
    }
}

