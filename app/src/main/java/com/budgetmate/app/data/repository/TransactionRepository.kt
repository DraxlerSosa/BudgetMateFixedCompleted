package com.budgetmate.app.data.repository

import android.util.Log
import com.budgetmate.app.data.dao.CategoryTotal
import com.budgetmate.app.data.dao.TransactionDao
import com.budgetmate.app.data.entity.TransactionEntity
import com.budgetmate.app.util.isEndTimeAfterStart
import kotlinx.coroutines.flow.Flow

/**
 * Repository for all transaction operations.
 * Validates data before writing and handles all error logging.
 */
class TransactionRepository(private val dao: TransactionDao) {

    companion object {
        private const val TAG = "TransactionRepository"
    }

    sealed class Result {
        data class Success(val id: Long) : Result()
        object InvalidAmount : Result()
        object InvalidTime : Result()
        object MissingCategory : Result()
        object MissingDate : Result()
    }

    suspend fun addTransaction(t: TransactionEntity): Result {
        if (t.amount <= 0.0) return Result.InvalidAmount
        if (t.categoryId <= 0) return Result.MissingCategory
        if (t.date.isBlank()) return Result.MissingDate
        if (!isEndTimeAfterStart(t.startTime, t.endTime)) return Result.InvalidTime
        val id = dao.insertTransaction(t)
        Log.i(TAG, "Transaction saved: ID=$id amount=${t.amount} date=${t.date}")
        return Result.Success(id)
    }

    suspend fun updateTransaction(t: TransactionEntity): Result {
        if (t.amount <= 0.0) return Result.InvalidAmount
        if (!isEndTimeAfterStart(t.startTime, t.endTime)) return Result.InvalidTime
        dao.updateTransaction(t)
        Log.i(TAG, "Transaction updated: ID=${t.transactionId}")
        return Result.Success(t.transactionId.toLong())
    }

    suspend fun deleteTransaction(t: TransactionEntity) {
        dao.deleteTransaction(t)
        Log.i(TAG, "Transaction deleted: ID=${t.transactionId}")
    }

    fun observeTransactions(userId: Int, start: String, end: String): Flow<List<TransactionEntity>> =
        dao.observeTransactionsByDateRange(userId, start, end)

    fun observeCategoryTotals(userId: Int, start: String, end: String): Flow<List<CategoryTotal>> =
        dao.observeCategoryTotals(userId, start, end)

    fun observeMonthlyTotal(userId: Int, year: String, month: String): Flow<Double> =
        dao.observeMonthlyTotal(userId, year, month)

    suspend fun getTransactionCount(userId: Int) = dao.getTransactionCount(userId)
    suspend fun getRecentDates(userId: Int) = dao.getRecentTransactionDates(userId)
}