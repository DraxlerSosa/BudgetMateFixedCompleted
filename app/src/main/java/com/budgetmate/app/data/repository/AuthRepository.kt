package com.budgetmate.app.data.repository

import android.util.Log
import com.budgetmate.app.data.dao.CategoryDao
import com.budgetmate.app.data.dao.UserDao
import com.budgetmate.app.data.entity.CategoryEntity
import com.budgetmate.app.data.entity.UserEntity
import com.budgetmate.app.util.sha256

/**
 * Handles all authentication logic: registration, login, and default category seeding.
 * Passwords are hashed before any database write — plaintext is never stored.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val categoryDao: CategoryDao
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    sealed class AuthResult {
        data class Success(val user: UserEntity) : AuthResult()
        object InvalidCredentials : AuthResult()
        object UsernameTaken : AuthResult()
        object EmptyFields : AuthResult()
        object PasswordTooShort : AuthResult()
    }

    /** Registers a new user and seeds their default categories. */
    suspend fun register(username: String, password: String, displayName: String): AuthResult {
        if (username.isBlank() || password.isBlank()) return AuthResult.EmptyFields
        if (password.length < 6) return AuthResult.PasswordTooShort
        if (userDao.getUserByUsername(username.trim()) != null) return AuthResult.UsernameTaken

        val user = UserEntity(
            username = username.trim(),
            passwordHash = password.sha256(),
            displayName = displayName.trim().ifBlank { username.trim() }
        )
        val id = userDao.insertUser(user).toInt()
        val created = userDao.getUserById(id)!!
        Log.i(TAG, "New user registered: ${created.username} (ID=$id)")
        seedDefaultCategories(id)
        return AuthResult.Success(created)
    }

    /** Logs in a user by validating username + hashed password. */
    suspend fun login(username: String, password: String): AuthResult {
        if (username.isBlank() || password.isBlank()) return AuthResult.EmptyFields
        val user = userDao.login(username.trim(), password.sha256())
        return if (user != null) {
            Log.i(TAG, "Login success: ${user.username}")
            AuthResult.Success(user)
        } else {
            Log.d(TAG, "Login failed for: $username")
            AuthResult.InvalidCredentials
        }
    }

    /** Seeds 10 default categories for a newly registered user. */
    private suspend fun seedDefaultCategories(userId: Int) {
        listOf(
            CategoryEntity(userId = userId, name = "Groceries",     iconEmoji = "🛒", colourHex = "#00C9A7", isDefault = true),
            CategoryEntity(userId = userId, name = "Transport",     iconEmoji = "🚗", colourHex = "#00B4D8", isDefault = true),
            CategoryEntity(userId = userId, name = "Utilities",     iconEmoji = "💡", colourHex = "#FFB703", isDefault = true),
            CategoryEntity(userId = userId, name = "Entertainment", iconEmoji = "🎬", colourHex = "#8338EC", isDefault = true),
            CategoryEntity(userId = userId, name = "Dining Out",    iconEmoji = "🍽️", colourHex = "#FB5607", isDefault = true),
            CategoryEntity(userId = userId, name = "Health",        iconEmoji = "❤️", colourHex = "#E63946", isDefault = true),
            CategoryEntity(userId = userId, name = "Shopping",      iconEmoji = "🛍️", colourHex = "#F72585", isDefault = true),
            CategoryEntity(userId = userId, name = "Savings",       iconEmoji = "💰", colourHex = "#2DC653", isDefault = true),
            CategoryEntity(userId = userId, name = "Education",     iconEmoji = "📚", colourHex = "#3A86FF", isDefault = true),
            CategoryEntity(userId = userId, name = "Other",         iconEmoji = "📌", colourHex = "#6C757D", isDefault = true),
        ).forEach { categoryDao.insertCategory(it) }
        Log.d(TAG, "Seeded 10 default categories for userId=$userId")
    }
}