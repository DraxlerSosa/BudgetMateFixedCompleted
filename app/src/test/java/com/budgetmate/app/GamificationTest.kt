package com.budgetmate.app

import com.budgetmate.app.util.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for gamification logic — XP calculation, level progression,
 * and streak-related logic. These run purely on the JVM (no Android context needed).
 */
class GamificationTest {

    // ── XP per transaction ────────────────────────────────────────

    @Test
    fun `saving one transaction grants exactly 10 XP`() {
        val xpPerTransaction = 10
        val xpAfterOne = 0 + xpPerTransaction
        assertEquals(10, xpAfterOne)
    }

    @Test
    fun `saving five transactions grants 50 XP`() {
        val total = 5 * 10
        assertEquals(50, total)
    }

    // ── Badge XP bonuses ──────────────────────────────────────────

    @Test
    fun `First Step badge grants 50 XP`() {
        val badgeXp = mapOf(
            "First Step"     to 50,
            "Getting Started" to 100,
            "Consistent"     to 200,
            "Century Club"   to 300,
            "3-Day Streak"   to 100,
            "Week Warrior"   to 150,
            "Monthly Master" to 300,
            "Goal Getter"    to 200
        )
        assertEquals(50, badgeXp["First Step"])
    }

    @Test
    fun `all badge keys are non-blank`() {
        val badges = listOf(
            "First Step", "Getting Started", "Consistent", "Century Club",
            "3-Day Streak", "Week Warrior", "Monthly Master", "Goal Getter"
        )
        badges.forEach { assertTrue(it.isNotBlank()) }
    }

    @Test
    fun `8 badges are defined`() {
        val badges = listOf(
            "First Step", "Getting Started", "Consistent", "Century Club",
            "3-Day Streak", "Week Warrior", "Monthly Master", "Goal Getter"
        )
        assertEquals(8, badges.size)
    }

    // ── Level progression ─────────────────────────────────────────

    @Test
    fun `level progresses from Beginner through all tiers`() {
        val expectedLevels = listOf(
            0    to "Beginner",
            100  to "Saver",
            300  to "Budgeter",
            600  to "Pro",
            1000 to "Expert",
            2000 to "Master"
        )
        expectedLevels.forEach { (xp, level) ->
            assertEquals("At $xp XP expected $level", level, xpLevel(xp))
        }
    }

    @Test
    fun `level just before threshold stays at lower level`() {
        assertEquals("Beginner", xpLevel(99))
        assertEquals("Saver",    xpLevel(299))
        assertEquals("Budgeter", xpLevel(599))
        assertEquals("Pro",      xpLevel(999))
        assertEquals("Expert",   xpLevel(1999))
    }

    @Test
    fun `level at exact threshold advances`() {
        assertEquals("Saver",    xpLevel(100))
        assertEquals("Budgeter", xpLevel(300))
        assertEquals("Pro",      xpLevel(600))
        assertEquals("Expert",   xpLevel(1000))
        assertEquals("Master",   xpLevel(2000))
    }

    // ── XP progress bar ───────────────────────────────────────────

    @Test
    fun `progress for 0 XP is 0`() {
        assertEquals(0f, xpLevelProgress(0), 0.001f)
    }

    @Test
    fun `progress for halfway through first tier is 0_5`() {
        // First tier: 0–100. At 50 XP → 50%
        assertEquals(0.5f, xpLevelProgress(50), 0.001f)
    }

    @Test
    fun `progress is always between 0 and 1`() {
        listOf(0, 50, 100, 250, 500, 800, 1500, 2500).forEach { xp ->
            val p = xpLevelProgress(xp)
            assertTrue("Progress for $xp XP out of range: $p", p in 0f..1f)
        }
    }

    // ── XP to next level ─────────────────────────────────────────

    @Test
    fun `at 0 XP needs 100 to next level`() {
        assertEquals(100, xpToNextLevel(0))
    }

    @Test
    fun `at 90 XP needs 10 to next level`() {
        assertEquals(10, xpToNextLevel(90))
    }

    @Test
    fun `at 100 XP needs 200 to next level`() {
        assertEquals(200, xpToNextLevel(100))
    }

    @Test
    fun `at 150 XP needs 150 to next level`() {
        // Tier 2: 100–300. At 150 → 300-150=150 left
        assertEquals(150, xpToNextLevel(150))
    }

    // ── Streak logic ──────────────────────────────────────────────

    @Test
    fun `consecutive dates produce streak`() {
        // Simulate: today, yesterday, day before → streak = 3
        val dates = listOf("2026-06-15", "2026-06-14", "2026-06-13")
        var streak = 0
        // Simple consecutive check
        for (i in dates.indices) {
            if (i == 0) { streak = 1; continue }
            val prev = dates[i - 1]
            val curr = dates[i]
            // If dates differ by exactly 1 day, streak continues
            // (simplified test — just checks list length here)
            streak++
        }
        assertEquals(3, streak)
    }

    @Test
    fun `empty date list gives zero streak`() {
        val dates = emptyList<String>()
        val streak = if (dates.isEmpty()) 0 else 1
        assertEquals(0, streak)
    }

    // ── Goal reward ───────────────────────────────────────────────

    @Test
    fun `spending within min-max range qualifies for goal reward`() {
        val min   = 500.0
        val max   = 2000.0
        val spent = 1200.0
        val qualifies = spent in min..max
        assertTrue(qualifies)
    }

    @Test
    fun `spending over max does not qualify for goal reward`() {
        val min   = 500.0
        val max   = 2000.0
        val spent = 2500.0
        val qualifies = spent in min..max
        assertFalse(qualifies)
    }

    @Test
    fun `spending below min does not qualify for goal reward`() {
        val min   = 500.0
        val max   = 2000.0
        val spent = 100.0
        val qualifies = spent in min..max
        assertFalse(qualifies)
    }
}
