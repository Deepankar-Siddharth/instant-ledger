package com.instantledger.data.preferences

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    
    @Provides
    @Singleton
    fun provideEncryptedSettingsPreferences(
        @ApplicationContext context: Context
    ): EncryptedSettingsPreferences {
        return EncryptedSettingsPreferences(context)
    }
}
