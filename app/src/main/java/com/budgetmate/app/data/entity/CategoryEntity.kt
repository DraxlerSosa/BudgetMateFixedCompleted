package com.budgetmate.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a spending category created by a user.
 * Each category belongs to one user and can have an optional monthly budget cap.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryId: Int = 0,
    val userId: Int,
    val name: String,
    val iconEmoji: String = "💰",
    val colourHex: String = "#00C9A7",
    val monthlyBudgetCap: Double? = null,
    val isDefault: Boolean = false
)