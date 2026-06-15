package com.budgetmate.app.data.repository

import android.util.Log
import com.budgetmate.app.data.dao.BadgeDao
import com.budgetmate.app.data.dao.UserDao
import com.budgetmate.app.data.entity.BadgeEntity
import com.budgetmate.app.util.today
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Handles all gamification: XP awarding, streak calculation, and badge evaluation.
 * Called after every successful transaction save and on monthly goal completion.
 */
class GamificationRepository(
    private val userDao: UserDao,
    private val badgeDao: BadgeDao,
    private val transactionRepo: TransactionRepository
) {
    companion object {
        private const val TAG = "GamificationRepository"
        const val XP_TRANSACTION = 10
        const val XP_STREAK_BONUS = 5
        const val XP_GOAL_MET = 100
    }

    data class GamEvent(
        val xpEarned: Int,
        val newBadges: List<BadgeEntity>,
        val streakUpdated: Boolean,
        val newStreak: Int
    )

    /** Called after every successful transaction save. */
    suspend fun onTransactionSaved(userId: Int): GamEvent {
        val todayStr = today()
        val user = userDao.getUserById(userId) ?: return GamEvent(0, emptyList(), false, 0)
        val recentDates = transactionRepo.getRecentDates(userId)

        // Streak
        val (newStreak, streakUpdated) = calcStreak(user.streak, user.lastLogDate, todayStr, recentDates)
        if (streakUpdated) {
            userDao.updateStreak(userId, newStreak, todayStr)
            Log.d(TAG, "Streak → $newStreak for userId=$userId")
        }

        // XP
        var xp = XP_TRANSACTION
        if (streakUpdated && newStreak > 1) xp += XP_STREAK_BONUS * newStreak
        userDao.addXp(userId, xp)
        Log.i(TAG, "+$xp XP for userId=$userId (streak=$newStreak)")

        val badges = evaluateBadges(userId, newStreak)
        return GamEvent(xp, badges, streakUpdated, newStreak)
    }

    suspend fun onGoalMet(userId: Int) {
        userDao.addXp(userId, XP_GOAL_MET)
        award(userId, "GOAL_MET", "Goal Getter", "🎯", XP_GOAL_MET)
    }

    private suspend fun evaluateBadges(userId: Int, streak: Int): List<BadgeEntity> {
        val earned = mutableListOf<BadgeEntity>()
        val count = transactionRepo.getTransactionCount(userId)
        award(userId, "FIRST_TRANSACTION",    "First Step",       "👣", 50)?.let  { if (count == 1)   earned.add(it) }
        award(userId, "TEN_TRANSACTIONS",     "Getting Started",  "🌱", 50)?.let  { if (count == 10)  earned.add(it) }
        award(userId, "FIFTY_TRANSACTIONS",   "Consistent",       "⭐", 100)?.let { if (count == 50)  earned.add(it) }
        award(userId, "HUNDRED_TRANSACTIONS", "Century Club",     "💯", 200)?.let { if (count == 100) earned.add(it) }
        if (streak >= 3)  award(userId, "STREAK_3",  "3-Day Streak",   "🔥", 30)?.let  { earned.add(it) }
        if (streak >= 7)  award(userId, "STREAK_7",  "Week Warrior",   "🗓️", 75)?.let  { earned.add(it) }
        if (streak >= 30) award(userId, "STREAK_30", "Monthly Master", "🏆", 300)?.let { earned.add(it) }
        return earned
    }

    /** Inserts a badge only if not already earned. Returns null if already owned. */
    private suspend fun award(userId: Int, key: String, name: String, emoji: String, xp: Int): BadgeEntity? {
        if (badgeDao.hasBadge(userId, key) > 0) return null
        val badge = BadgeEntity(userId = userId, badgeKey = key, badgeName = name,
            badgeEmoji = emoji, earnedDate = today(), xpReward = xp)
        badgeDao.insertBadge(badge)
        userDao.addXp(userId, xp)
        Log.i(TAG, "Badge earned: $name for userId=$userId")
        return badge
    }

    private fun calcStreak(current: Int, lastDate: String, today: String, dates: List<String>): Pair<Int, Boolean> {
        if (lastDate == today) return Pair(current, false)
        val yesterday = try { LocalDate.parse(today).minusDays(1).toString() } catch (e: Exception) { "" }
        return if (dates.size >= 2 && dates[1] == yesterday) Pair(current + 1, true)
        else Pair(1, true)
    }

    fun observeBadges(userId: Int): Flow<List<BadgeEntity>> = badgeDao.observeBadgesForUser(userId)
    fun observeBadgeCount(userId: Int) = badgeDao.observeBadgeCount(userId)
    fun observeXp(userId: Int) = userDao.observeXp(userId)
    fun observeStreak(userId: Int) = userDao.observeStreak(userId)
}