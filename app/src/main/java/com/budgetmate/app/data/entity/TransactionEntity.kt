package com.budgetmate.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single financial transaction (expense or income).
 * Captures all fields required by the module brief:
 * date, start time, end time, description, category, and optional photo URI.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [Index("userId"), Index("categoryId"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val transactionId: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val amount: Double,
    /** ISO-8601 date string (yyyy-MM-dd). Indexed for fast range queries. */
    val date: String,
    /** 24-hour start time (HH:mm) */
    val startTime: String,
    /** 24-hour end time (HH:mm) — must be after startTime */
    val endTime: String,
    val description: String? = null,
    /** Absolute file URI of the attached receipt photo. Null if none. */
    val photoUri: String? = null,
    val type: String = "EXPENSE"
)