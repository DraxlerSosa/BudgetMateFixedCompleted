package com.budgetmate.app.ui.categories

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.data.entity.CategoryEntity
import kotlinx.coroutines.launch

/**
 * ViewModel for the Categories screen and the AddCategoryBottomSheet.
 * Shared ViewModel approach: both the fragment and the bottom sheet can call load()
 * and it's idempotent — the userId is stored once and all operations use it.
 */
class CategoriesViewModel(application: Application) : AndroidViewModel(application) {

    companion object { private const val TAG = "CategoriesViewModel" }

    private val app = application as BudgetMateApp
    private var userId = -1

    private val _categories = MutableLiveData<List<CategoryEntity>>(emptyList())
    val categories: LiveData<List<CategoryEntity>> = _categories

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Idempotent: safe to call multiple times; sets userId only the first time. */
    fun load(uid: Int) {
        if (userId == uid && userId != -1) {
            Log.d(TAG, "load() called again for same userId=$uid — skipping re-subscribe")
            return
        }
        userId = uid
        Log.d(TAG, "Loading categories for userId=$userId")
        viewModelScope.launch {
            app.database.categoryDao().observeCategoriesForUser(userId).collect {
                Log.d(TAG, "Categories updated: ${it.size} items")
                _categories.value = it
            }
        }
    }

    fun addCategory(name: String, emoji: String, colour: String, cap: Double?) {
        if (userId == -1) { _error.value = "Not logged in"; return }
        if (name.isBlank()) { _error.value = "Name cannot be empty"; return }
        if (name.length > 30) { _error.value = "Name must be 30 characters or less"; return }

        viewModelScope.launch {
            // Check for duplicate name (excluding current category for edits)
            if (app.database.categoryDao().countByName(userId, name.trim()) > 0) {
                _error.value = "'${name.trim()}' already exists"; return@launch
            }
            app.database.categoryDao().insertCategory(
                CategoryEntity(
                    userId = userId,
                    name = name.trim(),
                    iconEmoji = emoji,
                    colourHex = colour,
                    monthlyBudgetCap = cap
                )
            )
            Log.i(TAG, "Category added: $name for userId=$userId")
        }
    }

    /**
     * Updates an existing category.
     * FIX: uses this ViewModel's stored userId, not the one passed in from the bottom sheet
     * (which was incorrectly 0 in the old version, causing the 5.4/10 category bug).
     */
    fun updateCategory(cat: CategoryEntity, name: String, emoji: String, colour: String, cap: Double?) {
        if (userId == -1) { _error.value = "Not logged in"; return }
        viewModelScope.launch {
            app.database.categoryDao().updateCategory(
                cat.copy(
                    userId = userId, // Always use the ViewModel's own userId
                    name = name.trim(),
                    iconEmoji = emoji,
                    colourHex = colour,
                    monthlyBudgetCap = cap
                )
            )
            Log.i(TAG, "Category updated: $name (ID=${cat.categoryId})")
        }
    }

    fun deleteCategory(cat: CategoryEntity) {
        viewModelScope.launch {
            val count = app.database.categoryDao().transactionCountForCategory(cat.categoryId)
            if (count > 0) {
                _error.value = "Cannot delete '${cat.name}' — it has $count transaction(s)"
                Log.w(TAG, "Delete blocked: ${cat.name} has $count transactions")
            } else {
                app.database.categoryDao().deleteCategory(cat)
                Log.i(TAG, "Category deleted: ${cat.name} (ID=${cat.categoryId})")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
