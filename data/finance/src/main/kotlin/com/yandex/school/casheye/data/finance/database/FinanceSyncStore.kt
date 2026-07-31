package com.yandex.school.casheye.data.finance.database

import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import java.time.Instant

/** Transactional outbox boundary used by synchronization to complete sent operations and refresh cache. */
internal interface FinanceSyncStore {
    suspend fun getPendingOperations(): List<PendingOperationEntity>
    suspend fun completeAccountCreate(sentOperations: List<PendingOperationEntity>, response: AccountDto)
    suspend fun completeAccountUpdate(sentOperations: List<PendingOperationEntity>, response: AccountDto)
    suspend fun completeTransactionCreate(sentOperations: List<PendingOperationEntity>, response: TransactionDto)
    suspend fun completeTransactionUpdate(sentOperations: List<PendingOperationEntity>, response: TransactionResponseDto)
    suspend fun completeDelete(sentOperations: List<PendingOperationEntity>)

    suspend fun refreshAfterSync(
        accounts: List<AccountDto>,
        categories: List<CategoryDto>,
        transactions: List<TransactionResponseDto>,
        startInclusive: Instant,
        endInclusive: Instant,
    )
}
