package com.budgetmate.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a registered user stored locally in Room.
 * Passwords are SHA-256 hashed — never stored in plaintext.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val username: String,
    val passwordHash: String,
    val displayName: String = username,
    val xpTotal: Int = 0,
    val streak: Int = 0,
    val lastLogDate: String = ""
)