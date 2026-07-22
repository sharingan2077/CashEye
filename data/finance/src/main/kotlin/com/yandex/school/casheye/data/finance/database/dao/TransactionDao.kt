package com.yandex.school.casheye.data.finance.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.yandex.school.casheye.data.finance.database.entity.TransactionEntity
import com.yandex.school.casheye.data.finance.database.entity.TransactionWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Int): TransactionWithRelations?

    @Transaction
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:accountId IS NULL OR account_id = :accountId)
          AND transaction_date BETWEEN :startInclusive AND :endInclusive
        ORDER BY transaction_date DESC
        """,
    )
    fun observeForPeriod(
        accountId: Int?,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<TransactionWithRelations>>

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Upsert
    suspend fun upsertAll(transactions: List<TransactionEntity>)
}
