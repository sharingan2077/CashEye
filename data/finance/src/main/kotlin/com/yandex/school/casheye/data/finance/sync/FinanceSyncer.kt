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
import com.yandex.school.casheye.data.finance.network.ServerRetryPolicy
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
import kotlin.time.Duration.Companion.milliseconds

sealed interface FinanceSyncResult {
    data object Success : FinanceSyncResult

    data class TemporaryFailure(
        val cause: Throwable,
    ) : FinanceSyncResult

    data class PermanentFailure(
        val cause: Throwable,
    ) : FinanceSyncResult
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
    private val waitBeforeRetry: suspend (Long) -> Unit = { delay(it.milliseconds) },
) {
    private val mutex = Mutex()
    private val retryPolicy = ServerRetryPolicy(waitBeforeRetry)

    suspend fun sync(): FinanceSyncResult = mutex.withLock { syncLocked() }

    private suspend fun syncLocked(): FinanceSyncResult =
        try {
            drainOutbox()
            refreshAllHistory()
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
        if (batch.operationType == PendingOperationType.DELETE) {
            deleteIfPresent { api.deleteAccount(batch.latest.localEntityId) }
            store.completeDelete(batch.operations)
            return
        }
        val snapshot = json.decodeFromString<AccountCommandSnapshot>(batch.latest.payload)
        val request =
            AccountRequestDto(
                name = snapshot.name,
                emoji = snapshot.emoji,
                balance = snapshot.balance,
                currency = snapshot.currency,
            )
        if (batch.isCreate) {
            val response = retryPolicy.execute { api.createAccount(request) }
            store.completeAccountCreate(batch.operations, response)
        } else {
            val response = retryPolicy.execute { api.updateAccount(snapshot.id, request) }
            store.completeAccountUpdate(batch.operations, response)
        }
    }

    private suspend fun sendTransaction(batch: PendingBatch) {
        if (batch.operationType == PendingOperationType.DELETE) {
            deleteIfPresent { api.deleteTransaction(batch.latest.localEntityId) }
            store.completeDelete(batch.operations)
            return
        }
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
            val response = retryPolicy.execute { api.createTransaction(request) }
            store.completeTransactionCreate(batch.operations, response)
        } else {
            val response = retryPolicy.execute { api.updateTransaction(snapshot.id, request) }
            store.completeTransactionUpdate(batch.operations, response)
        }
    }

    private suspend fun refreshAllHistory() {
        val today = LocalDate.now(clock)
        val accounts = retryPolicy.execute { api.getAccounts() }
        val categories =
            retryPolicy.execute { api.getCategories(true) } +
                retryPolicy.execute { api.getCategories(false) }
        val transactions =
            accounts.flatMap { account ->
                val startDate =
                    account.createdAt
                        .atZone(clock.zone)
                        .toLocalDate()
                        .coerceAtMost(today)
                retryPolicy.execute {
                    api.getTransactions(account.id, startDate.toString(), today.toString())
                }
            }
        val zone = clock.zone
        val startDate =
            accounts.minOfOrNull { account ->
                account.createdAt
                    .atZone(zone)
                    .toLocalDate()
                    .coerceAtMost(today)
            } ?: today
        store.refreshAfterSync(
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            startInclusive = startDate.atStartOfDay(zone).toInstant(),
            endInclusive =
                today
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .minusMillis(1),
        )
    }

    private suspend fun deleteIfPresent(block: suspend () -> Unit) {
        try {
            retryPolicy.execute(block)
        } catch (error: HttpException) {
            if (error.code() != HTTP_NOT_FOUND) throw error
        }
    }

    private data class PendingBatch(
        val operations: List<PendingOperationEntity>,
    ) {
        val latest: PendingOperationEntity = operations.maxWith(compareBy({ it.createdAt }, { it.id }))
        val entityType: PendingEntityType = latest.entityType
        val operationType: PendingOperationType = latest.operationType
        val isCreate: Boolean = operations.any { it.operationType == PendingOperationType.CREATE }
    }

    private fun List<PendingOperationEntity>.toBatches(): List<PendingBatch> =
        groupBy { it.entityType to it.localEntityId }
            .values
            .map(::PendingBatch)
            .sortedWith(
                compareBy<PendingBatch>(
                    { it.priority },
                    { it.operations.minOf(PendingOperationEntity::createdAt) },
                    { it.operations.minOf(PendingOperationEntity::id) },
                ),
            )

    private val PendingBatch.priority: Int
        get() =
            when {
                operationType == PendingOperationType.DELETE && entityType == PendingEntityType.TRANSACTION -> {
                    TRANSACTION_DELETE_PRIORITY
                }

                operationType == PendingOperationType.DELETE -> {
                    ACCOUNT_DELETE_PRIORITY
                }

                entityType == PendingEntityType.ACCOUNT -> {
                    ACCOUNT_WRITE_PRIORITY
                }

                else -> {
                    TRANSACTION_WRITE_PRIORITY
                }
            }

    private companion object {
        const val TRANSACTION_DELETE_PRIORITY = 0
        const val ACCOUNT_DELETE_PRIORITY = 1
        const val ACCOUNT_WRITE_PRIORITY = 2
        const val TRANSACTION_WRITE_PRIORITY = 3
        const val HTTP_NOT_FOUND = 404
        val SERVER_ERROR_RANGE = 500..599
    }
}
