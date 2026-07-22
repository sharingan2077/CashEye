package com.yandex.school.casheye.data.finance.sync

import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceSyncStore
import com.yandex.school.casheye.data.finance.database.entity.PendingEntityType
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationType
import com.yandex.school.casheye.data.finance.database.model.AccountCommandSnapshot
import com.yandex.school.casheye.data.finance.database.model.TransactionCommandSnapshot
import com.yandex.school.casheye.data.finance.dto.AccountBriefDto
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.AccountRequestDto
import com.yandex.school.casheye.data.finance.dto.AccountResponseDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionDto
import com.yandex.school.casheye.data.finance.dto.TransactionRequestDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FinanceSyncerTest {
    @Test
    fun `sync collapses updates and sends accounts before transactions`() =
        runBlocking {
            val store =
                FakeSyncStore(
                    accountUpdate(id = 1, operationId = 1, balance = "10.00", createdAt = 1),
                    transactionUpdate(id = 2, operationId = 2, createdAt = 2),
                    accountUpdate(id = 1, operationId = 3, balance = "25.00", createdAt = 3),
                )
            val api = FakeFinanceApi()

            val result = syncer(api, store).sync()

            assertEquals(FinanceSyncResult.Success, result)
            assertEquals(listOf("account:25.00", "transaction:2"), api.writeCalls)
            assertEquals(2, store.completedBatches.size)
            assertEquals(listOf(1L, 3L), store.completedBatches.first().map { it.id })
            assertTrue(store.refreshed)
        }

    @Test
    fun `server errors make three attempts with two second delays`() =
        runBlocking {
            val operation = accountUpdate(id = 1, operationId = 1, balance = "10.00", createdAt = 1)
            val store = FakeSyncStore(operation)
            val api = FakeFinanceApi(writeFailure = httpError(500))
            val delays = mutableListOf<Long>()

            val result = syncer(api, store) { delays += it }.sync()

            assertTrue(result is FinanceSyncResult.TemporaryFailure)
            assertEquals(3, api.accountUpdateAttempts)
            assertEquals(listOf(2_000L, 2_000L), delays)
            assertEquals(listOf(operation), store.getPendingOperations())
        }

    @Test
    fun `client errors are not retried and keep the operation`() =
        runBlocking {
            val operation = accountUpdate(id = 1, operationId = 1, balance = "10.00", createdAt = 1)
            val store = FakeSyncStore(operation)
            val api = FakeFinanceApi(writeFailure = httpError(400))

            val result = syncer(api, store).sync()

            assertTrue(result is FinanceSyncResult.PermanentFailure)
            assertEquals(1, api.accountUpdateAttempts)
            assertEquals(listOf(operation), store.getPendingOperations())
        }

    private fun syncer(
        api: FinanceApi,
        store: FinanceSyncStore,
        waitBeforeRetry: suspend (Long) -> Unit = {},
    ): FinanceSyncer =
        FinanceSyncer(
            api = api,
            store = store,
            clock = Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC),
            waitBeforeRetry = waitBeforeRetry,
        )

    private fun accountUpdate(
        id: Int,
        operationId: Long,
        balance: String,
        createdAt: Long,
    ): PendingOperationEntity =
        PendingOperationEntity(
            id = operationId,
            entityType = PendingEntityType.ACCOUNT,
            operationType = PendingOperationType.UPDATE,
            localEntityId = id,
            relatedAccountId = id,
            dependsOnOperationId = null,
            createdAt = createdAt,
            payload =
                Json.encodeToString(
                    AccountCommandSnapshot(
                        id = id,
                        name = "Account",
                        emoji = "💰",
                        balance = balance,
                        currency = "RUB",
                    ),
                ),
        )

    private fun transactionUpdate(
        id: Int,
        operationId: Long,
        createdAt: Long,
    ): PendingOperationEntity =
        PendingOperationEntity(
            id = operationId,
            entityType = PendingEntityType.TRANSACTION,
            operationType = PendingOperationType.UPDATE,
            localEntityId = id,
            relatedAccountId = 1,
            dependsOnOperationId = null,
            createdAt = createdAt,
            payload =
                Json.encodeToString(
                    TransactionCommandSnapshot(
                        id = id,
                        accountId = 1,
                        categoryId = 1,
                        amount = "5.00",
                        transactionDate = Instant.parse("2026-07-22T10:00:00Z").toEpochMilli(),
                        comment = null,
                    ),
                ),
        )

    private fun httpError(code: Int): HttpException =
        HttpException(Response.error<Unit>(code, "error".toResponseBody()))

    private class FakeSyncStore(
        vararg operations: PendingOperationEntity,
    ) : FinanceSyncStore {
        private val pending = operations.toMutableList()
        val completedBatches = mutableListOf<List<PendingOperationEntity>>()
        var refreshed = false

        override suspend fun getPendingOperations(): List<PendingOperationEntity> = pending.toList()

        override suspend fun completeAccountCreate(
            sentOperations: List<PendingOperationEntity>,
            response: AccountDto,
        ) = complete(sentOperations)

        override suspend fun completeAccountUpdate(
            sentOperations: List<PendingOperationEntity>,
            response: AccountDto,
        ) = complete(sentOperations)

        override suspend fun completeTransactionCreate(
            sentOperations: List<PendingOperationEntity>,
            response: TransactionDto,
        ) = complete(sentOperations)

        override suspend fun completeTransactionUpdate(
            sentOperations: List<PendingOperationEntity>,
            response: TransactionResponseDto,
        ) = complete(sentOperations)

        override suspend fun refreshAfterSync(
            accounts: List<AccountDto>,
            categories: List<CategoryDto>,
            transactions: List<TransactionResponseDto>,
            startInclusive: Instant,
            endInclusive: Instant,
        ) {
            refreshed = true
        }

        private fun complete(sentOperations: List<PendingOperationEntity>) {
            completedBatches += sentOperations
            pending.removeAll { operation -> sentOperations.any { it.id == operation.id } }
        }
    }

    private class FakeFinanceApi(
        private val writeFailure: Exception? = null,
    ) : FinanceApi {
        val writeCalls = mutableListOf<String>()
        var accountUpdateAttempts = 0

        override suspend fun getAccounts(): List<AccountDto> = emptyList()

        override suspend fun getAccount(id: Int): AccountResponseDto = error("Not used")

        override suspend fun createAccount(request: AccountRequestDto): AccountDto = error("Not used")

        override suspend fun updateAccount(
            id: Int,
            request: AccountRequestDto,
        ): AccountDto {
            accountUpdateAttempts += 1
            writeFailure?.let { throw it }
            writeCalls += "account:${request.balance}"
            return account(id, request)
        }

        override suspend fun getCategories(isIncome: Boolean): List<CategoryDto> = emptyList()

        override suspend fun getTransaction(id: Int): TransactionResponseDto = error("Not used")

        override suspend fun createTransaction(request: TransactionRequestDto): TransactionDto = error("Not used")

        override suspend fun updateTransaction(
            id: Int,
            request: TransactionRequestDto,
        ): TransactionResponseDto {
            writeCalls += "transaction:$id"
            return transaction(id, request)
        }

        override suspend fun getTransactions(
            accountId: Int,
            startDate: String,
            endDate: String,
        ): List<TransactionResponseDto> = emptyList()

        private fun account(
            id: Int,
            request: AccountRequestDto,
        ): AccountDto =
            AccountDto(
                id = id,
                userId = 1,
                name = request.name,
                emoji = request.emoji,
                balance = request.balance,
                currency = request.currency,
                createdAt = NOW,
                updatedAt = NOW,
            )

        private fun transaction(
            id: Int,
            request: TransactionRequestDto,
        ): TransactionResponseDto =
            TransactionResponseDto(
                id = id,
                account = AccountBriefDto(1, "Account", "💰", "100.00", "RUB"),
                category = CategoryDto(1, "Food", "🍔", false),
                amount = request.amount,
                transactionDate = request.transactionDate,
                comment = request.comment,
                createdAt = NOW,
                updatedAt = NOW,
            )

        private companion object {
            val NOW: Instant = Instant.parse("2026-07-22T12:00:00Z")
        }
    }
}
