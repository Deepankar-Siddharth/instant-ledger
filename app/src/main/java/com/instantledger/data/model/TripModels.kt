package com.instantledger.data.model

import com.instantledger.data.database.entities.TripEntity
import com.instantledger.data.database.entities.TripParticipantEntity

data class Trip(
    val id: Long,
    val name: String
)

data class TripParticipant(
    val id: Long,
    val tripId: Long,
    val name: String,
    val contact: String?
)

fun TripEntity.toDomain(): Trip = Trip(
    id = id,
    name = name
)

fun TripParticipantEntity.toDomain(): TripParticipant = TripParticipant(
    id = id,
    tripId = tripId,
    name = name,
    contact = contact
)

