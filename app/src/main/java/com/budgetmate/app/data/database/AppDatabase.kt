package com.budgetmate.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.budgetmate.app.data.dao.*
import com.budgetmate.app.data.entity.*

/**
 * Single Room database instance for BudgetMate.
 * All data stored locally — nothing leaves the device.
 * Singleton pattern prevents multiple instances opening simultaneously.
 */
@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BadgeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budgetmate_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}