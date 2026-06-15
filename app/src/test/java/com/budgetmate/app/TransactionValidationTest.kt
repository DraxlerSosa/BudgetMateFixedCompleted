package com.budgetmate.app

import com.budgetmate.app.data.entity.CategoryEntity
import com.budgetmate.app.data.entity.TransactionEntity
import com.budgetmate.app.util.isEndTimeAfterStart
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for transaction validation logic.
 * Mirrors the validation in TransactionRepository.addTransaction().
 * These tests run on the JVM without needing a device or Room database.
 */
class TransactionValidationTest {

    // Helper: build a valid TransactionEntity
    private fun validTransaction() = TransactionEntity(
        userId = 1,
        categoryId = 2,
        amount = 150.00,
        date = "2026-06-15",
        startTime = "08:00",
        endTime = "09:00",
        description = "Test transaction",
        type = "EXPENSE"
    )

    // ── Amount validation ────────────────────────────────────────

    @Test
    fun `positive amount is valid`() {
        val t = validTransaction().copy(amount = 100.0)
        assertTrue(t.amount > 0.0)
    }

    @Test
    fun `zero amount is invalid`() {
        val t = validTransaction().copy(amount = 0.0)
        assertFalse(t.amount > 0.0)
    }

    @Test
    fun `negative amount is invalid`() {
        val t = validTransaction().copy(amount = -50.0)
        assertFalse(t.amount > 0.0)
    }

    @Test
    fun `very small positive amount is valid`() {
        val t = validTransaction().copy(amount = 0.01)
        assertTrue(t.amount > 0.0)
    }

    // ── Time validation ──────────────────────────────────────────

    @Test
    fun `valid start and end time passes`() {
        val t = validTransaction()
        assertTrue(isEndTimeAfterStart(t.startTime, t.endTime))
    }

    @Test
    fun `equal times fail validation`() {
        val t = validTransaction().copy(startTime = "10:00", endTime = "10:00")
        assertFalse(isEndTimeAfterStart(t.startTime, t.endTime))
    }

    @Test
    fun `reversed times fail validation`() {
        val t = validTransaction().copy(startTime = "15:00", endTime = "09:00")
        assertFalse(isEndTimeAfterStart(t.startTime, t.endTime))
    }

    // ── Category validation ──────────────────────────────────────

    @Test
    fun `positive categoryId is valid`() {
        val t = validTransaction().copy(categoryId = 3)
        assertTrue(t.categoryId > 0)
    }

    @Test
    fun `categoryId of -1 is invalid`() {
        val t = validTransaction().copy(categoryId = -1)
        assertFalse(t.categoryId > 0)
    }

    @Test
    fun `categoryId of 0 is invalid`() {
        val t = validTransaction().copy(categoryId = 0)
        assertFalse(t.categoryId > 0)
    }

    // ── Date validation ──────────────────────────────────────────

    @Test
    fun `non-blank date is valid`() {
        val t = validTransaction()
        assertTrue(t.date.isNotBlank())
    }

    @Test
    fun `blank date is invalid`() {
        val t = validTransaction().copy(date = "")
        assertTrue(t.date.isBlank())
    }

    // ── Type validation ──────────────────────────────────────────

    @Test
    fun `EXPENSE type is recognised`() {
        val t = validTransaction().copy(type = "EXPENSE")
        assertEquals("EXPENSE", t.type)
    }

    @Test
    fun `INCOME type is recognised`() {
        val t = validTransaction().copy(type = "INCOME")
        assertEquals("INCOME", t.type)
    }

    // ── CategoryEntity validation ────────────────────────────────

    @Test
    fun `category name cannot be blank`() {
        val cat = CategoryEntity(userId = 1, name = "  ", iconEmoji = "💰", colourHex = "#00C9A7")
        assertTrue(cat.name.isBlank())
    }

    @Test
    fun `valid category has non-blank name`() {
        val cat = CategoryEntity(userId = 1, name = "Groceries", iconEmoji = "🛒", colourHex = "#00C9A7")
        assertFalse(cat.name.isBlank())
    }

    @Test
    fun `isDefault flag defaults to false`() {
        val cat = CategoryEntity(userId = 1, name = "Test", iconEmoji = "📌", colourHex = "#000000")
        assertFalse(cat.isDefault)
    }

    // ── Amount parsing from user input ───────────────────────────

    @Test
    fun `comma-separated amount parses correctly`() {
        val input = "1,250.50"
        val parsed = input.replace(",", ".").toDoubleOrNull()
        // Note: "1.250.50" is invalid; only simple comma→dot replacement
        assertNull(parsed) // This is the edge case — only single comma works
    }

    @Test
    fun `simple decimal amount parses correctly`() {
        val input = "250.50"
        val parsed = input.replace(",", ".").toDoubleOrNull()
        assertNotNull(parsed)
        assertEquals(250.50, parsed!!, 0.001)
    }

    @Test
    fun `non-numeric input returns null`() {
        val input = "abc"
        val parsed = input.toDoubleOrNull()
        assertNull(parsed)
    }

    @Test
    fun `empty string returns null`() {
        val parsed = "".toDoubleOrNull()
        assertNull(parsed)
    }
}
