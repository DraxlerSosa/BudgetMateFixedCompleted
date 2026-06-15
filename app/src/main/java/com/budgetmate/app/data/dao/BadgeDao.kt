package com.budgetmate.app.data.dao

import androidx.room.*
import com.budgetmate.app.data.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

/** DAO for gamification badge read/write operations. */
@Dao
interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(badge: BadgeEntity): Long

    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY earnedDate DESC")
    fun observeBadgesForUser(userId: Int): Flow<List<BadgeEntity>>

    @Query("SELECT COUNT(*) FROM badges WHERE userId = :userId AND badgeKey = :key")
    suspend fun hasBadge(userId: Int, key: String): Int

    @Query("SELECT COUNT(*) FROM badges WHERE userId = :userId")
    fun observeBadgeCount(userId: Int): Flow<Int>
}