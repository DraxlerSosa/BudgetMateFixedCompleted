package com.budgetmate.app.ui.transactions

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.data.entity.CategoryEntity
import com.budgetmate.app.data.entity.TransactionEntity
import com.budgetmate.app.data.repository.GamificationRepository
import com.budgetmate.app.data.repository.TransactionRepository
import com.budgetmate.app.util.isEndTimeAfterStart
import com.budgetmate.app.util.today
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** ViewModel for the Add Transaction bottom sheet. All validation lives here. */
class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    companion object { private const val TAG = "AddTransactionViewModel" }

    private val app  = application as BudgetMateApp
    private val repo = TransactionRepository(app.database.transactionDao())
    private val gam  = GamificationRepository(app.database.userDao(), app.database.badgeDao(), repo)

    sealed class SaveState {
        object Idle : SaveState()
        object Loading : SaveState()
        data class Success(val event: GamificationRepository.GamEvent) : SaveState()
        object InvalidAmount : SaveState()
        object InvalidTime : SaveState()
        object MissingCategory : SaveState()
        object MissingDate : SaveState()
        data class Error(val msg: String) : SaveState()
    }

    private val _state = MutableLiveData<SaveState>(SaveState.Idle)
    val state: LiveData<SaveState> = _state

    val categories = MutableLiveData<List<CategoryEntity>>(emptyList())

    // Form state survives rotation
    var selectedDate: String = today()
    var selectedStartTime: String = "08:00"
    var selectedEndTime: String = "09:00"
    var selectedCategoryId: Int = -1
    var photoUri: Uri? = null
    var transactionType: String = "EXPENSE"

    fun loadCategories(userId: Int) {
        viewModelScope.launch {
            val cats = app.database.categoryDao().getCategoriesForUser(userId)
            categories.value = cats
            val lastId = app.sessionManager.lastCategoryId.first()
            selectedCategoryId = if (cats.any { it.categoryId == lastId }) lastId
            else cats.firstOrNull()?.categoryId ?: -1
        }
    }

    fun save(userId: Int, amountText: String, description: String) {
        Log.d(TAG, "Saving transaction for userId=$userId")
        _state.value = SaveState.Loading

        val amount = amountText.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0.0) { _state.value = SaveState.InvalidAmount; return }
        if (selectedCategoryId == -1)         { _state.value = SaveState.MissingCategory; return }
        if (selectedDate.isBlank())            { _state.value = SaveState.MissingDate; return }
        if (!isEndTimeAfterStart(selectedStartTime, selectedEndTime)) {
            _state.value = SaveState.InvalidTime; return
        }

        viewModelScope.launch {
            val t = TransactionEntity(
                userId = userId, categoryId = selectedCategoryId, amount = amount,
                date = selectedDate, startTime = selectedStartTime, endTime = selectedEndTime,
                description = description.trim().ifBlank { null },
                photoUri = photoUri?.toString(), type = transactionType
            )
            when (val r = repo.addTransaction(t)) {
                is TransactionRepository.Result.Success -> {
                    app.sessionManager.setLastCategoryId(selectedCategoryId)
                    val event = gam.onTransactionSaved(userId)
                    _state.value = SaveState.Success(event)
                }
                TransactionRepository.Result.InvalidAmount   -> _state.value = SaveState.InvalidAmount
                TransactionRepository.Result.InvalidTime     -> _state.value = SaveState.InvalidTime
                TransactionRepository.Result.MissingCategory -> _state.value = SaveState.MissingCategory
                TransactionRepository.Result.MissingDate     -> _state.value = SaveState.MissingDate
            }
        }
    }

    fun reset() { _state.value = SaveState.Idle }
}