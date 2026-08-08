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
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FinanceSyncerTest {
    @Test
    fun `dependent transaction is sent after temporary account id is replaced`() =
        runBlocking {
            val account = accountCreate(localId = -1, operationId = 1, createdAt = 1)
            val transaction =
                transactionCreate(
                    localId = -1,
                    accountId = -1,
                    operationId = 2,
                    dependencyId = account.id,
                    createdAt = 2,
                )
            val store = FakeSyncStore(account, transaction)
            val api = FakeFinanceApi()

            val result = syncer(api, store).sync()

            assertEquals(FinanceSyncResult.Success, result)
            assertEquals(listOf("account:create", "transaction:create:101"), api.writeCalls)
            assertTrue(store.getPendingOperations().isEmpty())
        }

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

    @Test
    fun `network failure is temporary and keeps the operation for worker retry`() =
        runBlocking {
            val operation = accountUpdate(id = 1, operationId = 1, balance = "10.00", createdAt = 1)
            val store = FakeSyncStore(operation)
            val api = FakeFinanceApi(writeFailure = IOException("offline"))

            val result = syncer(api, store).sync()

            assertTrue(result is FinanceSyncResult.TemporaryFailure)
            assertEquals(1, api.accountUpdateAttempts)
            assertEquals(listOf(operation), store.getPendingOperations())
        }

    @Test
    fun `sync refreshes every account from creation date through today`() =
        runBlocking {
            val accounts =
                listOf(
                    serverAccount(id = 1, createdAt = Instant.parse("2025-02-03T18:00:00Z")),
                    serverAccount(id = 2, createdAt = Instant.parse("2026-06-15T08:00:00Z")),
                )
            val store = FakeSyncStore()
            val api = FakeFinanceApi(accounts = accounts)

            val result = syncer(api, store).sync()

            assertEquals(FinanceSyncResult.Success, result)
            assertEquals(
                listOf(
                    TransactionRead(1, "2025-02-03", "2026-07-22"),
                    TransactionRead(2, "2026-06-15", "2026-07-22"),
                ),
                api.transactionReads,
            )
            assertEquals(Instant.parse("2025-02-03T00:00:00Z"), store.refreshStart)
            assertEquals(Instant.parse("2026-07-22T23:59:59.999Z"), store.refreshEnd)
        }

    @Test
    fun `transaction deletes are sent before account delete`() =
        runBlocking {
            val store =
                FakeSyncStore(
                    deleteOperation(PendingEntityType.ACCOUNT, entityId = 1, operationId = 1),
                    deleteOperation(PendingEntityType.TRANSACTION, entityId = 10, operationId = 2),
                    deleteOperation(PendingEntityType.TRANSACTION, entityId = 11, operationId = 3),
                )
            val api = FakeFinanceApi()

            val result = syncer(api, store).sync()

            assertEquals(FinanceSyncResult.Success, result)
            assertEquals(
                listOf("transaction:delete:10", "transaction:delete:11", "account:delete:1"),
                api.writeCalls,
            )
            assertTrue(store.getPendingOperations().isEmpty())
        }

    @Test
    fun `missing remote entity completes delete idempotently`() =
        runBlocking {
            val store =
                FakeSyncStore(
                    deleteOperation(PendingEntityType.TRANSACTION, entityId = 10, operationId = 1),
                )
            val api = FakeFinanceApi(writeFailure = httpError(404))

            val result = syncer(api, store).sync()

            assertEquals(FinanceSyncResult.Success, result)
            assertTrue(store.getPendingOperations().isEmpty())
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

    private fun accountCreate(
        localId: Int,
        operationId: Long,
        createdAt: Long,
    ): PendingOperationEntity =
        accountUpdate(localId, operationId, "10.00", createdAt).copy(
            operationType = PendingOperationType.CREATE,
        )

    private fun transactionCreate(
        localId: Int,
        accountId: Int,
        operationId: Long,
        dependencyId: Long,
        createdAt: Long,
    ): PendingOperationEntity =
        transactionUpdate(localId, operationId, createdAt).copy(
            operationType = PendingOperationType.CREATE,
            relatedAccountId = accountId,
            dependsOnOperationId = dependencyId,
            payload =
                Json.encodeToString(
                    TransactionCommandSnapshot(
                        id = localId,
                        accountId = accountId,
                        categoryId = 1,
                        amount = "5.00",
                        transactionDate = Instant.parse("2026-07-22T10:00:00Z").toEpochMilli(),
                        comment = null,
                    ),
                ),
        )

    private fun deleteOperation(
        entityType: PendingEntityType,
        entityId: Int,
        operationId: Long,
    ): PendingOperationEntity =
        PendingOperationEntity(
            id = operationId,
            entityType = entityType,
            operationType = PendingOperationType.DELETE,
            localEntityId = entityId,
            relatedAccountId = 1,
            dependsOnOperationId = null,
            createdAt = operationId,
            payload = "",
        )

    private fun httpError(code: Int): HttpException =
        HttpException(Response.error<Unit>(code, "error".toResponseBody()))

    private class FakeSyncStore(
        vararg operations: PendingOperationEntity,
    ) : FinanceSyncStore {
        private val pending = operations.toMutableList()
        val completedBatches = mutableListOf<List<PendingOperationEntity>>()
        var refreshed = false
        var refreshStart: Instant? = null
        var refreshEnd: Instant? = null

        override suspend fun getPendingOperations(): List<PendingOperationEntity> = pending.toList()

        override suspend fun completeAccountCreate(
            sentOperations: List<PendingOperationEntity>,
            response: AccountDto,
        ) {
            complete(sentOperations)
            val completedIds = sentOperations.map { it.id }.toSet()
            pending.replaceAll { operation ->
                if (operation.dependsOnOperationId in completedIds) {
                    val snapshot = Json.decodeFromString<TransactionCommandSnapshot>(operation.payload)
                    operation.copy(
                        relatedAccountId = response.id,
                        dependsOnOperationId = null,
                        payload = Json.encodeToString(snapshot.copy(accountId = response.id)),
                    )
                } else {
                    operation
                }
            }
        }

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

        override suspend fun completeDelete(sentOperations: List<PendingOperationEntity>) = complete(sentOperations)

        override suspend fun refreshAfterSync(
            accounts: List<AccountDto>,
            categories: List<CategoryDto>,
            transactions: List<TransactionResponseDto>,
            startInclusive: Instant,
            endInclusive: Instant,
        ) {
            refreshed = true
            refreshStart = startInclusive
            refreshEnd = endInclusive
        }

        private fun complete(sentOperations: List<PendingOperationEntity>) {
            completedBatches += sentOperations
            pending.removeAll { operation -> sentOperations.any { it.id == operation.id } }
        }
    }

    private class FakeFinanceApi(
        private val writeFailure: Exception? = null,
        private val accounts: List<AccountDto> = emptyList(),
    ) : FinanceApi {
        val writeCalls = mutableListOf<String>()
        val transactionReads = mutableListOf<TransactionRead>()
        var accountUpdateAttempts = 0

        override suspend fun getAccounts(): List<AccountDto> = accounts

        override suspend fun getAccount(id: Int): AccountResponseDto = error("Not used")

        override suspend fun createAccount(request: AccountRequestDto): AccountDto {
            writeFailure?.let { throw it }
            writeCalls += "account:create"
            return account(101, request)
        }

        override suspend fun updateAccount(
            id: Int,
            request: AccountRequestDto,
        ): AccountDto {
            accountUpdateAttempts += 1
            writeFailure?.let { throw it }
            writeCalls += "account:${request.balance}"
            return account(id, request)
        }

        override suspend fun deleteAccount(id: Int) {
            writeFailure?.let { throw it }
            writeCalls += "account:delete:$id"
        }

        override suspend fun getCategories(isIncome: Boolean): List<CategoryDto> = emptyList()

        override suspend fun getTransaction(id: Int): TransactionResponseDto = error("Not used")

        override suspend fun createTransaction(request: TransactionRequestDto): TransactionDto {
            writeFailure?.let { throw it }
            writeCalls += "transaction:create:${request.accountId}"
            return TransactionDto(
                id = 201,
                accountId = request.accountId,
                categoryId = request.categoryId,
                amount = request.amount,
                transactionDate = request.transactionDate,
                comment = request.comment,
                createdAt = NOW,
                updatedAt = NOW,
            )
        }

        override suspend fun updateTransaction(
            id: Int,
            request: TransactionRequestDto,
        ): TransactionResponseDto {
            writeCalls += "transaction:$id"
            return transaction(id, request)
        }

        override suspend fun deleteTransaction(id: Int) {
            writeFailure?.let { throw it }
            writeCalls += "transaction:delete:$id"
        }

        override suspend fun getTransactions(
            accountId: Int,
            startDate: String,
            endDate: String,
        ): List<TransactionResponseDto> {
            transactionReads += TransactionRead(accountId, startDate, endDate)
            return emptyList()
        }

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

    private fun serverAccount(
        id: Int,
        createdAt: Instant,
    ): AccountDto =
        AccountDto(
            id = id,
            userId = 1,
            name = "Account $id",
            emoji = "💰",
            balance = "100.00",
            currency = "RUB",
            createdAt = createdAt,
            updatedAt = NOW,
        )

    private data class TransactionRead(
        val accountId: Int,
        val startDate: String,
        val endDate: String,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-22T12:00:00Z")
    }
}
