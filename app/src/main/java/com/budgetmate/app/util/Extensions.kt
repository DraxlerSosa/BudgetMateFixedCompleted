package com.budgetmate.app.util

import android.util.Log
import android.view.View
import android.widget.Toast
import android.content.Context
import com.google.android.material.snackbar.Snackbar
import java.security.MessageDigest
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "BudgetMateUtils"

// ── Security ──────────────────────────────────────────────────────

/** Hashes a password string with SHA-256. Plaintext is never stored. */
fun String.sha256(): String {
    return try {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        bytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        Log.e(TAG, "SHA-256 hashing failed", e)
        this
    }
}

// ── Formatting ────────────────────────────────────────────────────

/** Formats a Double as ZAR currency (e.g. R 1 250,00). */
fun Double.toZar(): String {
    return try {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        formatter.format(this)
    } catch (e: Exception) {
        "R %.2f".format(this)
    }
}

/** Converts an ISO-8601 date string to a readable format (e.g. 28 Apr 2026). */
fun String.toReadableDate(): String {
    return try {
        val date = LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (e: Exception) {
        this
    }
}

/** Returns today as ISO-8601 string (yyyy-MM-dd). */
fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

/** Returns the first day of the current month as ISO-8601. */
fun firstDayOfMonth(): String = YearMonth.now().atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

/** Returns the last day of the current month as ISO-8601. */
fun lastDayOfMonth(): String = YearMonth.now().atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE)

/**
 * Returns true if endTime (HH:mm) is strictly after startTime (HH:mm).
 * Used to validate transaction time fields before saving.
 */
fun isEndTimeAfterStart(start: String, end: String): Boolean {
    return try {
        val (sh, sm) = start.split(":").map { it.toInt() }
        val (eh, em) = end.split(":").map { it.toInt() }
        (eh * 60 + em) > (sh * 60 + sm)
    } catch (e: Exception) {
        false
    }
}

// ── View helpers ──────────────────────────────────────────────────

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }

fun View.snack(message: String, duration: Int = Snackbar.LENGTH_SHORT) =
    Snackbar.make(this, message, duration).show()

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

/** Parses a hex colour string to an Android color Int (handles # prefix). */
fun String.toColorInt(): Int = android.graphics.Color.parseColor(
    if (startsWith("#")) this else "#$this"
)

// ── XP / Gamification helpers ─────────────────────────────────────

/** Returns a level name for a given XP total. */
fun xpLevel(xp: Int): String = when {
    xp < 100  -> "Beginner"
    xp < 300  -> "Saver"
    xp < 600  -> "Budgeter"
    xp < 1000 -> "Pro"
    xp < 2000 -> "Expert"
    else      -> "Master"
}

/** Returns how many XP are needed to reach the next level. */
fun xpToNextLevel(xp: Int): Int {
    val thresholds = listOf(100, 300, 600, 1000, 2000, Int.MAX_VALUE)
    return (thresholds.firstOrNull { it > xp } ?: Int.MAX_VALUE) - xp
}

/** Returns level progress as 0..1 Float for a ProgressBar. */
fun xpLevelProgress(xp: Int): Float {
    val starts = listOf(0, 100, 300, 600, 1000, 2000)
    val ends   = listOf(100, 300, 600, 1000, 2000, 3000)
    val idx    = starts.indexOfLast { xp >= it }.coerceAtLeast(0)
    val range  = (ends[idx] - starts[idx]).toFloat()
    val prog   = (xp - starts[idx]).toFloat()
    return (prog / range).coerceIn(0f, 1f)
}