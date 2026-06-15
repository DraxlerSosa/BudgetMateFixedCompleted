package com.budgetmate.app.data.dao

import androidx.room.*
import com.budgetmate.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** Result model for category total aggregation queries. */
data class CategoryTotal(
    val categoryId: Int,
    val categoryName: String,
    val iconEmoji: String,
    val colourHex: String,
    val total: Double
)

/** DAO for all transaction read/write operations. */
@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE transactionId = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    /**
     * Returns all transactions for a user within a date range.
     * Powers the Reports screen date-range filter (FR-06).
     */
    @Query("""
        SELECT * FROM transactions
        WHERE userId = :userId AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, startTime DESC
    """)
    fun observeTransactionsByDateRange(
        userId: Int, startDate: String, endDate: String
    ): Flow<List<TransactionEntity>>

    /**
     * Returns sum of amounts grouped by category for a date range.
     * Powers the pie chart and category totals list (FR-06).
     */
    @Query("""
        SELECT t.categoryId, c.name AS categoryName, c.iconEmoji, c.colourHex,
               SUM(t.amount) AS total
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.categoryId
        WHERE t.userId = :userId AND t.date BETWEEN :startDate AND :endDate
        AND t.type = 'EXPENSE'
        GROUP BY t.categoryId ORDER BY total DESC
    """)
    fun observeCategoryTotals(
        userId: Int, startDate: String, endDate: String
    ): Flow<List<CategoryTotal>>

    /**
     * Monthly expense total for the dashboard goal indicator (FR-05).
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE userId = :userId
        AND strftime('%Y', date) = :year
        AND strftime('%m', date) = :month
        AND type = 'EXPENSE'
    """)
    fun observeMonthlyTotal(userId: Int, year: String, month: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId")
    suspend fun getTransactionCount(userId: Int): Int

    /** Used for streak calculation in GamificationRepository. */
    @Query("SELECT DISTINCT date FROM transactions WHERE userId = :userId ORDER BY date DESC LIMIT 30")
    suspend fun getRecentTransactionDates(userId: Int): List<String>
}