package com.budgetmate.app.data.dao

import androidx.room.*
import com.budgetmate.app.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for all user-related database operations.
 * All suspend functions run on a background coroutine — never the main thread.
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(username: String, passwordHash: String): UserEntity?

    @Query("UPDATE users SET xpTotal = xpTotal + :xp WHERE userId = :userId")
    suspend fun addXp(userId: Int, xp: Int)

    @Query("UPDATE users SET streak = :streak, lastLogDate = :date WHERE userId = :userId")
    suspend fun updateStreak(userId: Int, streak: Int, date: String)

    @Query("SELECT xpTotal FROM users WHERE userId = :userId")
    fun observeXp(userId: Int): Flow<Int>

    @Query("SELECT streak FROM users WHERE userId = :userId")
    fun observeStreak(userId: Int): Flow<Int>
}