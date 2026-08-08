package com.yandex.school.casheye.data.finance.database

import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import java.time.Instant

/** Adapts Room's internal outbox operations to the narrow synchronization contract. */
internal class RoomFinanceSyncStore(
    private val localStore: RoomFinanceLocalStore,
) : FinanceSyncStore {
    override suspend fun getPendingOperations(): List<PendingOperationEntity> = localStore.getPendingOperations()

    override suspend fun completeAccountCreate(
        sentOperations: List<PendingOperationEntity>,
        response: AccountDto,
    ) = localStore.completeAccountCreate(sentOperations, response)

    override suspend fun completeAccountUpdate(
        sentOperations: List<PendingOperationEntity>,
        response: AccountDto,
    ) = localStore.completeAccountUpdate(sentOperations, response)

    override suspend fun completeTransactionCreate(
        sentOperations: List<PendingOperationEntity>,
        response: TransactionDto,
    ) = localStore.completeTransactionCreate(sentOperations, response)

    override suspend fun completeTransactionUpdate(
        sentOperations: List<PendingOperationEntity>,
        response: TransactionResponseDto,
    ) = localStore.completeTransactionUpdate(sentOperations, response)

    override suspend fun completeDelete(sentOperations: List<PendingOperationEntity>) =
        localStore.completeDelete(sentOperations)

    override suspend fun refreshAfterSync(
        accounts: List<AccountDto>,
        categories: List<CategoryDto>,
        transactions: List<TransactionResponseDto>,
        startInclusive: Instant,
        endInclusive: Instant,
    ) = localStore.refreshPeriod(accounts, categories, transactions, startInclusive, endInclusive)
}
