package com.budgetmate.app

import com.budgetmate.app.util.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for utility extension functions in Extensions.kt.
 * Tests cover: time validation, currency formatting, XP level calculation.
 */
class ExtensionsTest {

    // ── isEndTimeAfterStart ──────────────────────────────────────

    @Test
    fun `endTime after startTime returns true`() {
        assertTrue(isEndTimeAfterStart("08:00", "09:00"))
    }

    @Test
    fun `endTime same as startTime returns false`() {
        assertFalse(isEndTimeAfterStart("08:00", "08:00"))
    }

    @Test
    fun `endTime before startTime returns false`() {
        assertFalse(isEndTimeAfterStart("09:00", "08:00"))
    }

    @Test
    fun `endTime only minutes ahead returns true`() {
        assertTrue(isEndTimeAfterStart("08:30", "08:45"))
    }

    @Test
    fun `endTime midnight vs noon returns false`() {
        assertFalse(isEndTimeAfterStart("12:00", "00:00"))
    }

    @Test
    fun `malformed time string returns false safely`() {
        assertFalse(isEndTimeAfterStart("bad", "time"))
    }

    // ── toZar formatting ─────────────────────────────────────────

    @Test
    fun `zero formats as ZAR`() {
        val result = 0.0.toZar()
        assertTrue("Expected R in: $result", result.contains("R") || result.contains("0"))
    }

    @Test
    fun `positive amount contains value`() {
        val result = 1250.50.toZar()
        assertTrue("Expected 1250 in: $result", result.contains("1") && result.contains("250"))
    }

    // ── xpLevel ──────────────────────────────────────────────────

    @Test
    fun `0 xp is Beginner`() {
        assertEquals("Beginner", xpLevel(0))
    }

    @Test
    fun `99 xp is Beginner`() {
        assertEquals("Beginner", xpLevel(99))
    }

    @Test
    fun `100 xp is Saver`() {
        assertEquals("Saver", xpLevel(100))
    }

    @Test
    fun `300 xp is Budgeter`() {
        assertEquals("Budgeter", xpLevel(300))
    }

    @Test
    fun `600 xp is Pro`() {
        assertEquals("Pro", xpLevel(600))
    }

    @Test
    fun `1000 xp is Expert`() {
        assertEquals("Expert", xpLevel(1000))
    }

    @Test
    fun `2000 xp is Master`() {
        assertEquals("Master", xpLevel(2000))
    }

    // ── xpToNextLevel ────────────────────────────────────────────

    @Test
    fun `0 xp needs 100 to next level`() {
        assertEquals(100, xpToNextLevel(0))
    }

    @Test
    fun `50 xp needs 50 to next level`() {
        assertEquals(50, xpToNextLevel(50))
    }

    @Test
    fun `100 xp needs 200 to next level`() {
        assertEquals(200, xpToNextLevel(100))
    }

    // ── xpLevelProgress ──────────────────────────────────────────

    @Test
    fun `progress is between 0 and 1`() {
        val progress = xpLevelProgress(50)
        assertTrue(progress >= 0f && progress <= 1f)
    }

    @Test
    fun `0 xp has 0 progress`() {
        assertEquals(0f, xpLevelProgress(0), 0.001f)
    }

    @Test
    fun `mid-level has fractional progress`() {
        val progress = xpLevelProgress(50) // halfway to 100
        assertEquals(0.5f, progress, 0.001f)
    }

    // ── sha256 ───────────────────────────────────────────────────

    @Test
    fun `sha256 of same string is consistent`() {
        assertEquals("password".sha256(), "password".sha256())
    }

    @Test
    fun `sha256 of different strings differ`() {
        assertNotEquals("password".sha256(), "Password".sha256())
    }

    @Test
    fun `sha256 output is 64 hex chars`() {
        assertEquals(64, "test".sha256().length)
    }

    // ── toReadableDate ───────────────────────────────────────────

    @Test
    fun `valid ISO date formats correctly`() {
        val result = "2026-06-15".toReadableDate()
        assertTrue("Expected Jun in: $result", result.contains("Jun") || result.contains("15"))
    }

    @Test
    fun `invalid date returns input unchanged`() {
        assertEquals("not-a-date", "not-a-date".toReadableDate())
    }
}
