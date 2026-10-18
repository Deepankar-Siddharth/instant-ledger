package com.instantledger.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantledger.data.model.*
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.data.repository.MerchantRepository
import com.instantledger.domain.parser.TransactionParser
import com.instantledger.domain.parser.SMSParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantRepository: MerchantRepository
) : ViewModel() {
    
    private val transactionParser = TransactionParser(SMSParser())
    
    fun getAllMerchants(): Flow<List<com.instantledger.data.database.entities.MerchantEntity>> {
        return merchantRepository.getAllMerchants()
    }
    
    suspend fun getMerchantSuggestionsSync(): List<String> {
        return transactionRepository.getAllUniqueMerchants()
    }
    
    fun saveTransaction(
        amount: Double,
        merchant: String,
        merchantOverride: String?,
        category: String?,
        paymentMode: PaymentMode,
        timestamp: Long,
        notes: String?,
        transactionType: TransactionType = TransactionType.DEBIT,
        existingTransaction: Transaction? = null
    ) {
        viewModelScope.launch {
            val transaction = if (existingTransaction != null) {
                // Update existing transaction - updatedAt will be set by @Update annotation
                existingTransaction.copy(
                    amount = amount,
                    merchant = merchant.ifBlank { "" },
                    merchantOverride = merchantOverride?.takeIf { it.isNotBlank() },
                    category = category,
                    paymentMode = paymentMode,
                    timestamp = timestamp,
                    notes = notes,
                    transactionType = transactionType,
                    entryType = EntryType.USER_MODIFIED,
                    updatedAt = System.currentTimeMillis() // This will be used by @Update
                )
            } else {
                // Create new manual entry - only auto-approve if category is provided
                transactionParser.parseManualEntry(
                    amount = amount,
                    merchant = merchant.ifBlank { "" },
                    category = category,
                    paymentMode = paymentMode,
                    timestamp = timestamp,
                    notes = notes,
                    transactionType = transactionType
                ).copy(
                    merchantOverride = merchantOverride?.takeIf { it.isNotBlank() },
                    isApproved = !category.isNullOrBlank() // Only auto-approve if category exists
                )
            }
            
            if (existingTransaction != null) {
                transactionRepository.updateTransaction(transaction)
            } else {
                transactionRepository.insertTransaction(transaction)
            }
        }
    }
}
