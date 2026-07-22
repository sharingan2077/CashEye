package com.yandex.school.casheye.data.finance.sync

import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceSyncStore
import com.yandex.school.casheye.data.finance.database.entity.PendingEntityType
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationType
import com.yandex.school.casheye.data.finance.database.model.AccountCommandSnapshot
import com.yandex.school.casheye.data.finance.database.model.TransactionCommandSnapshot
import com.yandex.school.casheye.data.finance.dto.AccountRequestDto
import com.yandex.school.casheye.data.finance.dto.TransactionRequestDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

sealed interface FinanceSyncResult {
    data object Success : FinanceSyncResult

    data class TemporaryFailure(val cause: Throwable) : FinanceSyncResult

    data class PermanentFailure(val cause: Throwable) : FinanceSyncResult
}

/**
 * Sends the durable outbox in local-wins order.
 *
 * POST requests are necessarily at-least-once: the API has no idempotency key, so a lost
 * successful response followed by a retry can create a duplicate server record.
 */
class FinanceSyncer internal constructor(
    private val api: FinanceApi,
    private val store: FinanceSyncStore,
    private val json: Json = Json,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val waitBeforeRetry: suspend (Long) -> Unit = { delay(it) },
) {
    private val mutex = Mutex()

    suspend fun sync(): FinanceSyncResult = mutex.withLock { syncLocked() }

    private suspend fun syncLocked(): FinanceSyncResult =
        try {
            drainOutbox()
            refreshCurrentMonth()
            FinanceSyncResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            FinanceSyncResult.TemporaryFailure(error)
        } catch (error: HttpException) {
            if (error.code() in SERVER_ERROR_RANGE) {
                FinanceSyncResult.TemporaryFailure(error)
            } else {
                FinanceSyncResult.PermanentFailure(error)
            }
        } catch (error: Exception) {
            FinanceSyncResult.PermanentFailure(error)
        }

    private suspend fun drainOutbox() {
        while (true) {
            val pending = store.getPendingOperations()
            if (pending.isEmpty()) return
            val readyBatches = pending.toBatches().filter { it.latest.dependsOnOperationId == null }
            check(readyBatches.isNotEmpty()) { "Pending operation dependency cycle detected" }

            readyBatches.forEach { batch -> send(batch) }
        }
    }

    private suspend fun send(batch: PendingBatch) {
        when (batch.entityType) {
            PendingEntityType.ACCOUNT -> sendAccount(batch)
            PendingEntityType.TRANSACTION -> sendTransaction(batch)
        }
    }

    private suspend fun sendAccount(batch: PendingBatch) {
        val snapshot = json.decodeFromString<AccountCommandSnapshot>(batch.latest.payload)
        val request =
            AccountRequestDto(
                name = snapshot.name,
                emoji = snapshot.emoji,
                balance = snapshot.balance,
                currency = snapshot.currency,
            )
        if (batch.isCreate) {
            val response = withServerRetry { api.createAccount(request) }
            store.completeAccountCreate(batch.operations, response)
        } else {
            val response = withServerRetry { api.updateAccount(snapshot.id, request) }
            store.completeAccountUpdate(batch.operations, response)
        }
    }

    private suspend fun sendTransaction(batch: PendingBatch) {
        val snapshot = json.decodeFromString<TransactionCommandSnapshot>(batch.latest.payload)
        val request =
            TransactionRequestDto(
                accountId = snapshot.accountId,
                categoryId = snapshot.categoryId,
                amount = snapshot.amount,
                transactionDate = Instant.ofEpochMilli(snapshot.transactionDate),
                comment = snapshot.comment,
            )
        if (batch.isCreate) {
            val response = withServerRetry { api.createTransaction(request) }
            store.completeTransactionCreate(batch.operations, response)
        } else {
            val response = withServerRetry { api.updateTransaction(snapshot.id, request) }
            store.completeTransactionUpdate(batch.operations, response)
        }
    }

    private suspend fun refreshCurrentMonth() {
        val today = LocalDate.now(clock)
        val startDate = today.withDayOfMonth(1)
        val endDate = today.withDayOfMonth(today.lengthOfMonth())
        val accounts = withServerRetry { api.getAccounts() }
        val categories =
            withServerRetry { api.getCategories(true) } +
                withServerRetry { api.getCategories(false) }
        val transactions =
            accounts.flatMap { account ->
                withServerRetry {
                    api.getTransactions(account.id, startDate.toString(), endDate.toString())
                }
            }
        val zone = clock.zone
        store.refreshAfterSync(
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            startInclusive = startDate.atStartOfDay(zone).toInstant(),
            endInclusive = endDate.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1),
        )
    }

    private suspend fun <T> withServerRetry(block: suspend () -> T): T {
        repeat(MAX_SERVER_ATTEMPTS - 1) {
            try {
                return block()
            } catch (error: HttpException) {
                if (error.code() !in SERVER_ERROR_RANGE) throw error
                waitBeforeRetry(RETRY_DELAY_MILLIS)
            }
        }
        return block()
    }

    private data class PendingBatch(
        val operations: List<PendingOperationEntity>,
    ) {
        val latest: PendingOperationEntity = operations.maxWith(compareBy({ it.createdAt }, { it.id }))
        val entityType: PendingEntityType = latest.entityType
        val isCreate: Boolean = operations.any { it.operationType == PendingOperationType.CREATE }
    }

    private fun List<PendingOperationEntity>.toBatches(): List<PendingBatch> =
        groupBy { it.entityType to it.localEntityId }
            .values
            .map(::PendingBatch)
            .sortedWith(
                compareBy<PendingBatch>(
                    { if (it.entityType == PendingEntityType.ACCOUNT) ACCOUNT_PRIORITY else TRANSACTION_PRIORITY },
                    { it.operations.minOf(PendingOperationEntity::createdAt) },
                    { it.operations.minOf(PendingOperationEntity::id) },
                ),
            )

    private companion object {
        const val MAX_SERVER_ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 2_000L
        const val ACCOUNT_PRIORITY = 0
        const val TRANSACTION_PRIORITY = 1
        val SERVER_ERROR_RANGE = 500..599
    }
}
