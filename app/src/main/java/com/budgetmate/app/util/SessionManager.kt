package com.budgetmate.app.util

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "budgetmate_prefs")

/**
 * Manages the logged-in user session and goal settings using Jetpack DataStore.
 * All reads are Flows so the UI reacts to changes automatically.
 */
class SessionManager(private val context: Context) {

    companion object {
        private val KEY_USER_ID   = intPreferencesKey("logged_in_user_id")
        private val KEY_MIN_GOAL  = doublePreferencesKey("min_monthly_goal")
        private val KEY_MAX_GOAL  = doublePreferencesKey("max_monthly_goal")
        private val KEY_LAST_CAT  = intPreferencesKey("last_category_id")
    }

    /** Emits -1 when no user is logged in. */
    val loggedInUserId: Flow<Int> = context.dataStore.data.map { it[KEY_USER_ID] ?: -1 }

    val minMonthlyGoal: Flow<Double> = context.dataStore.data.map { it[KEY_MIN_GOAL] ?: 0.0 }
    val maxMonthlyGoal: Flow<Double> = context.dataStore.data.map { it[KEY_MAX_GOAL] ?: 0.0 }
    val lastCategoryId: Flow<Int>    = context.dataStore.data.map { it[KEY_LAST_CAT] ?: -1 }

    suspend fun setLoggedInUser(userId: Int) =
        context.dataStore.edit { it[KEY_USER_ID] = userId }

    suspend fun logout() =
        context.dataStore.edit { it.remove(KEY_USER_ID) }

    suspend fun setGoals(min: Double, max: Double) =
        context.dataStore.edit { it[KEY_MIN_GOAL] = min; it[KEY_MAX_GOAL] = max }

    suspend fun setLastCategoryId(categoryId: Int) =
        context.dataStore.edit { it[KEY_LAST_CAT] = categoryId }
}