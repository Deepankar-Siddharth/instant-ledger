package com.instantledger.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.instantledger.data.model.ChangeSource
import com.instantledger.data.model.TransactionChangeLog

@Entity(tableName = "transaction_change_logs")
data class TransactionChangeLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,
    val field: String,
    @ColumnInfo(name = "old_value")
    val oldValue: String?,
    @ColumnInfo(name = "new_value")
    val newValue: String?,
    @ColumnInfo(name = "changed_at")
    val changedAt: Long,
    val source: String  // Stored as string, converted from enum
) {
    fun toDomain(): TransactionChangeLog {
        return TransactionChangeLog(
            id = id,
            transactionId = transactionId,
            field = field,
            oldValue = oldValue,
            newValue = newValue,
            changedAt = changedAt,
            source = ChangeSource.valueOf(source)
        )
    }
    
    companion object {
        fun fromDomain(changeLog: TransactionChangeLog): TransactionChangeLogEntity {
            return TransactionChangeLogEntity(
                id = changeLog.id,
                transactionId = changeLog.transactionId,
                field = changeLog.field,
                oldValue = changeLog.oldValue,
                newValue = changeLog.newValue,
                changedAt = changeLog.changedAt,
                source = changeLog.source.name
            )
        }
    }
}
