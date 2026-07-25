package com.yandex.school.casheye.data.finance.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yandex.school.casheye.data.finance.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_income = :isIncome ORDER BY name")
    suspend fun getByType(isIncome: Boolean): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Int): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)
}
