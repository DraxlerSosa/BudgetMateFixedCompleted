package com.budgetmate.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores a gamification badge earned by a user.
 * Badges are awarded automatically when milestones are reached.
 */
@Entity(
    tableName = "badges",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class BadgeEntity(
    @PrimaryKey(autoGenerate = true)
    val badgeId: Int = 0,
    val userId: Int,
    /** Unique key e.g. "FIRST_TRANSACTION", "STREAK_7", "GOAL_MET" */
    val badgeKey: String,
    val badgeName: String,
    val badgeEmoji: String,
    /** ISO-8601 date the badge was earned */
    val earnedDate: String,
    val xpReward: Int = 50
)