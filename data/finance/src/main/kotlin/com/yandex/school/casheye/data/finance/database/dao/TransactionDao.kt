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
    suspend fun getForPeriod(
        accountId: Int?,
        startInclusive: Long,
        endInclusive: Long,
    ): List<TransactionWithRelations>

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId")
    suspend fun countByAccountId(accountId: Int): Int

    @Query("SELECT currency FROM transactions WHERE id = :id")
    suspend fun getCurrencyById(id: Int): String?

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

    @Query(
        """
        DELETE FROM transactions
        WHERE id > 0
          AND transaction_date BETWEEN :startInclusive AND :endInclusive
          AND id NOT IN (
              SELECT local_entity_id FROM pending_operations
              WHERE entity_type = 'TRANSACTION'
          )
        """,
    )
    suspend fun deleteSyncedForPeriod(
        startInclusive: Long,
        endInclusive: Long,
    )
}
