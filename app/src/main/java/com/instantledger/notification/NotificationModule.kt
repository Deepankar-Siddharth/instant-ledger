package com.instantledger.notification

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.data.preferences.EncryptedSettingsPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    @Provides
    @Singleton
    fun provideTransactionNotificationManager(
        @ApplicationContext context: Context,
        transactionRepository: TransactionRepository,
        settingsPreferences: EncryptedSettingsPreferences
    ): TransactionNotificationManager {
        return TransactionNotificationManager(context, transactionRepository, settingsPreferences)
    }
}
