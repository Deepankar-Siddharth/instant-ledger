package com.instantledger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.instantledger.data.database.dao.TransactionDao
import com.instantledger.data.database.dao.GoalDao
import com.instantledger.data.database.dao.UnverifiedTransactionDao
import com.instantledger.data.database.dao.TransactionChangeLogDao
import com.instantledger.data.database.dao.CategoryDao
import com.instantledger.data.database.dao.TripDao
import com.instantledger.data.database.dao.MerchantDao
import com.instantledger.data.database.entities.TransactionEntity
import com.instantledger.data.database.entities.GoalEntity
import com.instantledger.data.database.entities.UnverifiedTransactionEntity
import com.instantledger.data.database.entities.TransactionChangeLogEntity
import com.instantledger.data.database.entities.CategoryEntity
import com.instantledger.data.database.entities.TripEntity
import com.instantledger.data.database.entities.TripParticipantEntity
import com.instantledger.data.database.entities.TripShareEntity
import com.instantledger.data.database.entities.MerchantEntity
import com.instantledger.security.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        TransactionEntity::class, 
        GoalEntity::class, 
        UnverifiedTransactionEntity::class,
        TransactionChangeLogEntity::class,
        CategoryEntity::class,
        TripEntity::class,
        TripParticipantEntity::class,
        TripShareEntity::class,
        MerchantEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun unverifiedTransactionDao(): UnverifiedTransactionDao
    abstract fun transactionChangeLogDao(): TransactionChangeLogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tripDao(): TripDao
    abstract fun merchantDao(): MerchantDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2: Add goals table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        target_amount REAL NOT NULL,
                        current_amount REAL NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        // Migration from version 2 to 3: Add transaction status, schema versioning, and quarantine table
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add transaction status column (defaults to CONFIRMED for existing records)
                db.execSQL("""
                    ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'CONFIRMED'
                """.trimIndent())
                
                // Add schema versioning fields
                db.execSQL("""
                    ALTER TABLE transactions ADD COLUMN schema_version INTEGER NOT NULL DEFAULT 1
                """.trimIndent())
                
                db.execSQL("""
                    ALTER TABLE transactions ADD COLUMN parser_version INTEGER NOT NULL DEFAULT 1
                """.trimIndent())
                
                // Add sender trust score
                db.execSQL("""
                    ALTER TABLE transactions ADD COLUMN sender_id TEXT
                """.trimIndent())
                
                db.execSQL("""
                    ALTER TABLE transactions ADD COLUMN sender_trust_score REAL
                """.trimIndent())
                
                // Create quarantine table for low-confidence transactions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS unverified_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        raw_text TEXT NOT NULL,
                        parsed_amount REAL,
                        parsed_merchant TEXT,
                        confidence_score REAL NOT NULL,
                        sender_id TEXT,
                        sender_trust_score REAL,
                        timestamp INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        // Migration from version 3 to 4: Add isApproved field
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add isApproved column (defaults to true for existing records to maintain backward compatibility)
                db.execSQL("""
                    ALTER TABLE transactions ADD COLUMN is_approved INTEGER NOT NULL DEFAULT 1
                """.trimIndent())
            }
        }
        
        // Migration from version 4 to 5: Add transaction change log table
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create transaction change log table for audit trail
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transaction_change_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        transaction_id INTEGER NOT NULL,
                        field TEXT NOT NULL,
                        old_value TEXT,
                        new_value TEXT,
                        changed_at INTEGER NOT NULL,
                        source TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Create index for faster queries
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_transaction_change_logs_transaction_id 
                    ON transaction_change_logs(transaction_id)
                """.trimIndent())
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_transaction_change_logs_changed_at 
                    ON transaction_change_logs(changed_at)
                """.trimIndent())
            }
        }
        
        // Migration from version 5 to 6: Add category versioning and categories table
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add category versioning fields to transactions
                db.execSQL("""
                    ALTER TABLE transactions ADD COLUMN category_id TEXT
                """.trimIndent())
                
                db.execSQL("""
                    ALTER TABLE transactions ADD COLUMN category_name_snapshot TEXT
                """.trimIndent())
                
                // Create categories table for versioned category management
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        semantic TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        is_active INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                
                // Create index for category lookups
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_categories_name 
                    ON categories(name)
                """.trimIndent())
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_categories_is_active 
                    ON categories(is_active)
                """.trimIndent())
                
                // Create index for transaction category lookups
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_transactions_category_id 
                    ON transactions(category_id)
                """.trimIndent())
            }
        }

        // Migration from version 6 to 7: Add trips, participants, shares, and trip_id column
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add nullable trip_id column to transactions
                db.execSQL(
                    """
                    ALTER TABLE transactions ADD COLUMN trip_id INTEGER
                    """.trimIndent()
                )

                // Create trips table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trips (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        is_active INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )

                // Create trip_participants table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trip_participants (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trip_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        contact TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_trip_participants_trip_id
                    ON trip_participants(trip_id)
                    """.trimIndent()
                )

                // Create trip_shares table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trip_shares (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trip_id INTEGER NOT NULL,
                        transaction_id INTEGER NOT NULL,
                        participant_id INTEGER NOT NULL,
                        share_amount REAL NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_trip_shares_trip_id
                    ON trip_shares(trip_id)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_trip_shares_transaction_id
                    ON trip_shares(transaction_id)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_trip_shares_participant_id
                    ON trip_shares(participant_id)
                    """.trimIndent()
                )
            }
        }

        // Migration from version 7 to 8: Add merchants table and merchant_override column
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create merchants table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS merchants (
                        original_name TEXT PRIMARY KEY NOT NULL,
                        display_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                
                // Add merchant_override column to transactions
                db.execSQL(
                    """
                    ALTER TABLE transactions ADD COLUMN merchant_override TEXT
                    """.trimIndent()
                )
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                android.util.Log.d("InstantLedger", "AppDatabase: Initializing database...")
                try {
                    val factory = EncryptionManager.createOpenHelperFactory(context)
                    android.util.Log.d("InstantLedger", "AppDatabase: OpenHelperFactory created")
                    
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "instant_ledger.db"
                    )
                        .openHelperFactory(factory)
                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8
                        )
                        .build()
                    
                    android.util.Log.d("InstantLedger", "AppDatabase: Database initialized successfully")
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    android.util.Log.e("InstantLedger", "AppDatabase: Failed to initialize database", e)
                    throw e
                }
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideTransactionDao(database: AppDatabase): com.instantledger.data.database.dao.TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    fun provideGoalDao(database: AppDatabase): com.instantledger.data.database.dao.GoalDao {
        return database.goalDao()
    }
    
    @Provides
    fun provideGoalRepository(goalDao: com.instantledger.data.database.dao.GoalDao): com.instantledger.data.repository.GoalRepository {
        return com.instantledger.data.repository.GoalRepository(goalDao)
    }
    
    @Provides
    fun provideUnverifiedTransactionDao(database: AppDatabase): com.instantledger.data.database.dao.UnverifiedTransactionDao {
        return database.unverifiedTransactionDao()
    }
    
    @Provides
    fun provideUnverifiedTransactionRepository(unverifiedTransactionDao: com.instantledger.data.database.dao.UnverifiedTransactionDao): com.instantledger.data.repository.UnverifiedTransactionRepository {
        return com.instantledger.data.repository.UnverifiedTransactionRepository(unverifiedTransactionDao)
    }
    
    @Provides
    fun provideTransactionChangeLogDao(database: AppDatabase): com.instantledger.data.database.dao.TransactionChangeLogDao {
        return database.transactionChangeLogDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): com.instantledger.data.database.dao.CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideTripDao(database: AppDatabase): com.instantledger.data.database.dao.TripDao {
        return database.tripDao()
    }
    
    @Provides
    fun provideMerchantDao(database: AppDatabase): com.instantledger.data.database.dao.MerchantDao {
        return database.merchantDao()
    }
    
    @Provides
    fun provideMerchantRepository(merchantDao: com.instantledger.data.database.dao.MerchantDao): com.instantledger.data.repository.MerchantRepository {
        return com.instantledger.data.repository.MerchantRepository(merchantDao)
    }
}
