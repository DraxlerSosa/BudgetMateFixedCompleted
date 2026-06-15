package com.budgetmate.app.data.dao

import androidx.room.*
import com.budgetmate.app.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/** DAO for category CRUD operations. */
@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun observeCategoriesForUser(userId: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    suspend fun getCategoriesForUser(userId: Int): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: Int): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId AND name = :name AND categoryId != :excludeId")
    suspend fun countByName(userId: Int, name: String, excludeId: Int = 0): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun transactionCountForCategory(categoryId: Int): Int
}