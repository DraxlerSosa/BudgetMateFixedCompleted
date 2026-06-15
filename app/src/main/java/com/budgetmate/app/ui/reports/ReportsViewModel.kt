package com.budgetmate.app.ui.reports

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.data.dao.CategoryTotal
import com.budgetmate.app.data.entity.CategoryEntity
import com.budgetmate.app.data.entity.TransactionEntity
import com.budgetmate.app.data.repository.TransactionRepository
import com.budgetmate.app.util.firstDayOfMonth
import com.budgetmate.app.util.lastDayOfMonth
import kotlinx.coroutines.launch

/**
 * ViewModel for the Reports screen.
 * Holds the selected date range and exposes filtered transactions,
 * category totals for the pie chart, running total, and goal values.
 */
class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    companion object { private const val TAG = "ReportsViewModel" }

    private val app  = application as BudgetMateApp
    private val repo = TransactionRepository(app.database.transactionDao())

    var startDate = firstDayOfMonth()
    var endDate   = lastDayOfMonth()

    // Goal values — populated from DataStore by the Fragment
    var minGoal: Double = 0.0
    var maxGoal: Double = 0.0

    private val _transactions   = MutableLiveData<List<TransactionEntity>>(emptyList())
    val transactions: LiveData<List<TransactionEntity>> = _transactions

    private val _categoryTotals = MutableLiveData<List<CategoryTotal>>(emptyList())
    val categoryTotals: LiveData<List<CategoryTotal>> = _categoryTotals

    private val _total          = MutableLiveData(0.0)
    val total: LiveData<Double> = _total

    private val _categoryMap    = MutableLiveData<Map<Int, CategoryEntity>>(emptyMap())
    val categoryMap: LiveData<Map<Int, CategoryEntity>> = _categoryMap

    fun load(userId: Int, start: String, end: String) {
        startDate = start
        endDate   = end
        Log.d(TAG, "Loading reports for userId=$userId from=$start to=$end")

        viewModelScope.launch {
            val cats = app.database.categoryDao().getCategoriesForUser(userId)
            _categoryMap.value = cats.associateBy { it.categoryId }
            Log.d(TAG, "Category map loaded: ${cats.size} categories")
        }

        viewModelScope.launch {
            repo.observeTransactions(userId, start, end).collect { list ->
                _transactions.value = list
                // Only sum EXPENSE type transactions toward the spending total
                _total.value = list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                Log.d(TAG, "Transactions: ${list.size}, total expense: ${_total.value}")
            }
        }

        viewModelScope.launch {
            repo.observeCategoryTotals(userId, start, end).collect { totals ->
                _categoryTotals.value = totals
                Log.d(TAG, "Category totals: ${totals.size} entries")
            }
        }
    }

    fun deleteTransaction(t: TransactionEntity) {
        viewModelScope.launch {
            repo.deleteTransaction(t)
            Log.i(TAG, "Deleted transaction ID=${t.transactionId}")
        }
    }
}
