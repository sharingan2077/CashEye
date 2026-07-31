package com.yandex.school.casheye.data.finance.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yandex.school.casheye.data.finance.database.entity.AccountTransactionHistoryVerificationEntity

@Dao
internal interface AccountTransactionHistoryVerificationDao {
    @Query("SELECT EXISTS(SELECT 1 FROM account_transaction_history_verifications WHERE account_id = :accountId)")
    suspend fun isVerified(accountId: Int): Boolean

    @Upsert
    suspend fun upsert(verification: AccountTransactionHistoryVerificationEntity)
}
