package com.yandex.school.casheye.data.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceLocalStore
import com.yandex.school.casheye.data.finance.dto.AccountBriefDto
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionDto
import com.yandex.school.casheye.data.finance.dto.TransactionRequestDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.data.finance.mapper.toDomain
import com.yandex.school.casheye.data.finance.repository.FinanceRepositoryImpl
import com.yandex.school.casheye.data.finance.sync.FinanceSyncScheduler
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceDataLoadResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.SaveTransactionCommand
import com.yandex.school.casheye.domain.finance.TransactionsQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Collections

class FinanceRepositoryImplTest {
    @Test
    fun `accounts observer returns cache without waiting for api`() =
        runBlocking {
            val cached = Account(7, "Cached", "💳", BigDecimal("25.00"), "RUB")
            val repository =
                FinanceRepositoryImpl(
                    ThrowingFinanceApi(IOException("offline")),
                    FakeFinanceLocalStore(initialAccounts = listOf(cached)),
                    Dispatchers.Unconfined,
                )

            assertEquals(listOf(cached), repository.observeAccounts().first())
        }

    @Test
    fun `successful accounts refresh updates observed cache`() =
        runBlocking {
            val localStore = FakeFinanceLocalStore()
            val repository =
                FinanceRepositoryImpl(
                    FakeFinanceApi(listOf(accountDto(1)), emptyMap()),
                    localStore,
                    Dispatchers.Unconfined,
                )

            assertEquals(FinanceRefreshResult.Success, repository.refreshAccounts())

            assertEquals(listOf(1), repository.observeAccounts().first().map { it.id })
        }

    @Test
    fun `failed accounts refresh preserves observed cache`() =
        runBlocking {
            val cached = Account(7, "Cached", "💳", BigDecimal("25.00"), "RUB")
            val repository =
                FinanceRepositoryImpl(
                    ThrowingFinanceApi(IOException("offline")),
                    FakeFinanceLocalStore(initialAccounts = listOf(cached)),
                    Dispatchers.Unconfined,
                )

            assertEquals(
                FinanceRefreshResult.Failure(
                    FinanceFailureReason.Network,
                    hasUsableCache = false,
                ),
                repository.refreshAccounts(),
            )
            assertEquals(listOf(cached), repository.observeAccounts().first())
        }

    @Test
    fun `failed refresh reports usable cache when categories were saved`() =
        runBlocking {
            val localStore = FakeFinanceLocalStore(hasUsableCache = true)

            val result =
                repository(
                    ThrowingFinanceApi(IOException("offline")),
                    localStore,
                ).refreshAccounts()

            assertEquals(
                FinanceRefreshResult.Failure(
                    FinanceFailureReason.Network,
                    hasUsableCache = true,
                ),
                result,
            )
        }

    @Test
    fun `cached accounts are returned when refresh is offline`() =
        runBlocking {
            val cached = Account(7, "Cached", "💳", BigDecimal("25.00"), "RUB")
            val localStore = FakeFinanceLocalStore(initialAccounts = listOf(cached))

            val result =
                FinanceRepositoryImpl(
                    ThrowingFinanceApi(IOException("offline")),
                    localStore,
                    Dispatchers.Unconfined,
                ).getAccounts()

            assertEquals(FinanceDataLoadResult.Success(listOf(cached)), result)
        }

    @Test
    fun `local account commit enqueues synchronization`() =
        runBlocking {
            val localStore = FakeFinanceLocalStore()
            val scheduler = RecordingSyncScheduler()
            val command = SaveAccountCommand(null, "Offline", "💳", BigDecimal("10.00"), "RUB")
            val repository =
                FinanceRepositoryImpl(
                    FakeFinanceApi(emptyList(), emptyMap()),
                    localStore,
                    Dispatchers.Unconfined,
                    scheduler,
                )

            val result = repository.saveAccount(command)

            assertEquals(EditorResult.Success(Unit), result)
            assertEquals(command, localStore.savedAccount)
            assertEquals(1, scheduler.immediateRequests)
        }

    @Test
    fun `editor maps io exception to network failure`() =
        runBlocking {
            val result =
                FinanceRepositoryImpl(
                    ThrowingFinanceApi(IOException("offline")),
                    FakeFinanceLocalStore(),
                    Dispatchers.Unconfined,
                ).getCategories(isIncome = false)

            assertEquals(EditorResult.Failure(FinanceFailureReason.Network), result)
        }

    @Test
    fun `create transaction maps domain command to request`() =
        runBlocking {
            val api = FakeFinanceApi(accounts = emptyList(), transactionsByAccount = emptyMap())
            val command =
                SaveTransactionCommand(
                    id = null,
                    accountId = 4,
                    categoryId = 8,
                    amount = BigDecimal("12.30"),
                    transactionDate = Instant.parse("2026-07-22T10:15:00Z"),
                    comment = "Обед",
                )

            val localStore = FakeFinanceLocalStore()
            val result = FinanceRepositoryImpl(api, localStore, Dispatchers.Unconfined).saveTransaction(command)

            assertTrue(result is EditorResult.Success)
            assertEquals("12.30", localStore.savedTransaction?.amount?.toPlainString())
            assertEquals(4, localStore.savedTransaction?.accountId)
            assertEquals(8, localStore.savedTransaction?.categoryId)
        }

    @Test
    fun `requests every account with same current-day boundaries`() =
        runBlocking {
            val api =
                FakeFinanceApi(
                    accounts = listOf(accountDto(1), accountDto(2), accountDto(3)),
                    transactionsByAccount = emptyMap(),
                )
            val date = LocalDate.of(2026, 7, 17)

            repository(api).getTransactions(TransactionsQuery(setOf(1, 2, 3), date, date))

            assertEquals(setOf(1, 2, 3), api.requests.map { it.accountId }.toSet())
            assertTrue(api.requests.all { it.startDate == "2026-07-17" })
            assertTrue(api.requests.all { it.endDate == "2026-07-17" })
        }

    @Test
    fun `returns transactions without applying domain filters or ordering`() =
        runBlocking {
            val api =
                FakeFinanceApi(
                    accounts = listOf(accountDto(1), accountDto(2)),
                    transactionsByAccount =
                        mapOf(
                            1 to
                                listOf(
                                    transactionDto(1, "10.25", Instant.parse("2026-07-17T08:00:00Z")),
                                    transactionDto(
                                        id = 2,
                                        amount = "500.00",
                                        transactionDate = Instant.parse("2026-07-17T09:00:00Z"),
                                        isIncome = true,
                                    ),
                                ),
                            2 to
                                listOf(
                                    transactionDto(3, "20.75", Instant.parse("2026-07-17T10:00:00Z")),
                                ),
                        ),
                )

            val result =
                repository(api).getTransactions(
                    TransactionsQuery(setOf(1, 2), LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)),
                ) as FinanceDataLoadResult.Success

            assertEquals(listOf(1, 2, 3), result.data.map { it.id })
        }

    @Test
    fun `returns empty account collection`() =
        runBlocking {
            val api = FakeFinanceApi(accounts = emptyList(), transactionsByAccount = emptyMap())

            val result = repository(api).getAccounts() as FinanceDataLoadResult.Success

            assertTrue(result.data.isEmpty())
            assertTrue(api.requests.isEmpty())
        }

    @Test
    fun `returns income transactions without filtering`() =
        runBlocking {
            val api =
                FakeFinanceApi(
                    accounts = listOf(accountDto(1)),
                    transactionsByAccount =
                        mapOf(
                            1 to
                                listOf(
                                    transactionDto(
                                        id = 1,
                                        amount = "100.00",
                                        transactionDate = Instant.parse("2026-07-17T10:00:00Z"),
                                        isIncome = true,
                                    ),
                                ),
                        ),
                )

            val result =
                repository(api).getTransactions(
                    TransactionsQuery(setOf(1), LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)),
                ) as FinanceDataLoadResult.Success

            assertEquals(listOf(1), result.data.map { it.id })
        }

    @Test
    fun `network exception maps to network failure`() =
        runBlocking {
            val api = ThrowingFinanceApi(IOException("offline"))

            val result = repository(api).getAccounts()

            assertEquals(
                FinanceDataLoadResult.Failure(FinanceFailureReason.Network),
                result,
            )
        }

    @Test
    fun `server response maps to server failure`() =
        runBlocking {
            val response = Response.error<Unit>(500, "server error".toResponseBody())
            val api = ThrowingFinanceApi(HttpException(response))

            val result = repository(api).getAccounts()

            assertEquals(
                FinanceDataLoadResult.Failure(FinanceFailureReason.Server),
                result,
            )
            assertEquals(3, api.accountRequests)
        }

    @Test
    fun `one failed account request fails the whole load`() =
        runBlocking {
            val api =
                FakeFinanceApi(
                    accounts = listOf(accountDto(1), accountDto(2)),
                    transactionsByAccount =
                        mapOf(
                            1 to
                                listOf(
                                    transactionDto(1, "10.00", Instant.parse("2026-07-17T10:00:00Z")),
                                ),
                        ),
                    failingAccountId = 2,
                )
            val localStore = FakeFinanceLocalStore()

            val result =
                repository(api, localStore).getTransactions(
                    TransactionsQuery(setOf(1, 2), LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)),
                )

            assertEquals(
                FinanceDataLoadResult.Failure(FinanceFailureReason.Network),
                result,
            )
            assertEquals(0, localStore.periodRefreshes)
        }
}

private fun repository(
    api: FinanceApi,
    localStore: FinanceLocalStore = FakeFinanceLocalStore(),
): FinanceRepositoryImpl =
    FinanceRepositoryImpl(
        api,
        localStore,
        Dispatchers.Unconfined,
        waitBeforeRetry = {},
    )

private class FakeFinanceLocalStore(
    initialAccounts: List<Account> = emptyList(),
    private val hasUsableCache: Boolean = false,
) : FinanceLocalStore {
    private val accounts = MutableStateFlow(initialAccounts)
    private var categories: List<Category> = emptyList()
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    var savedTransaction: SaveTransactionCommand? = null
    var savedAccount: SaveAccountCommand? = null
    var periodRefreshes: Int = 0

    override fun observeAccounts(): Flow<List<Account>> = accounts

    override fun observeTransactions(
        accountId: Int?,
        startInclusive: Instant,
        endInclusive: Instant,
    ): Flow<List<Transaction>> = transactions

    override suspend fun getAccounts(): List<Account> = accounts.value

    override suspend fun getAccount(id: Int): Account? = accounts.value.firstOrNull { it.id == id }

    override suspend fun getCategories(isIncome: Boolean): List<Category> =
        categories.filter { it.isIncome == isIncome }

    override suspend fun hasUsableCache(): Boolean = hasUsableCache || categories.isNotEmpty()

    override suspend fun getTransaction(id: Int): Transaction? = transactions.value.firstOrNull { it.id == id }

    override suspend fun getTransactions(
        accountId: Int?,
        startInclusive: Instant,
        endInclusive: Instant,
    ): List<Transaction> =
        transactions.value.filter {
            (accountId == null || it.account.id == accountId) &&
                it.transactionDate >= startInclusive &&
                it.transactionDate <= endInclusive
        }

    override suspend fun refreshAccounts(accounts: List<AccountDto>) {
        this.accounts.value = accounts.map { it.toDomain() }
    }

    override suspend fun refreshCategories(categories: List<CategoryDto>) {
        this.categories = categories.map { it.toDomain() }
    }

    override suspend fun refreshPeriod(
        accounts: List<AccountDto>,
        categories: List<CategoryDto>,
        transactions: List<TransactionResponseDto>,
        startInclusive: Instant,
        endInclusive: Instant,
    ) {
        periodRefreshes += 1
        this.accounts.value = accounts.map { it.toDomain() }
        this.categories = categories.map { it.toDomain() }
        this.transactions.value = transactions.map { it.toDomain() }
    }

    override suspend fun cacheAccount(account: com.yandex.school.casheye.data.finance.dto.AccountResponseDto) {
        accounts.value = accounts.value.filterNot { it.id == account.id } + account.toDomain()
    }

    override suspend fun cacheTransaction(transaction: TransactionResponseDto) {
        transactions.value = transactions.value.filterNot { it.id == transaction.id } + transaction.toDomain()
    }

    override suspend fun saveAccount(
        command: SaveAccountCommand,
        now: Instant,
    ) {
        savedAccount = command
    }

    override suspend fun saveTransaction(command: SaveTransactionCommand, now: Instant) {
        savedTransaction = command
    }

    override suspend fun getAccountTransactionCount(id: Int): Int =
        transactions.value.count { it.account.id == id }

    override suspend fun deleteTransaction(id: Int, now: Instant) {
        transactions.value = transactions.value.filterNot { it.id == id }
    }

    override suspend fun deleteAccount(id: Int, now: Instant): Int {
        val transactionCount = transactions.value.count { it.account.id == id }
        transactions.value = transactions.value.filterNot { it.account.id == id }
        accounts.value = accounts.value.filterNot { it.id == id }
        return transactionCount
    }
}

private class RecordingSyncScheduler : FinanceSyncScheduler {
    var immediateRequests = 0

    override fun registerPeriodicSync() = Unit

    override fun enqueueImmediateSync() {
        immediateRequests += 1
    }
}

private data class PeriodRequest(
    val accountId: Int,
    val startDate: String,
    val endDate: String,
)

private class FakeFinanceApi(
    private val accounts: List<AccountDto>,
    private val transactionsByAccount: Map<Int, List<TransactionResponseDto>>,
    private val failingAccountId: Int? = null,
) : FinanceApi {
    val requests: MutableList<PeriodRequest> = Collections.synchronizedList(mutableListOf())
    override suspend fun getAccounts(): List<AccountDto> = accounts

    override suspend fun getAccount(id: Int) = error("Unexpected account request")

    override suspend fun createAccount(request: com.yandex.school.casheye.data.finance.dto.AccountRequestDto) =
        accountDto(1)

    override suspend fun updateAccount(
        id: Int,
        request: com.yandex.school.casheye.data.finance.dto.AccountRequestDto,
    ) = accountDto(id)

    override suspend fun deleteAccount(id: Int) = Unit

    override suspend fun getCategories(isIncome: Boolean): List<CategoryDto> = emptyList()

    override suspend fun getTransaction(id: Int) = error("Unexpected transaction request")

    override suspend fun createTransaction(request: TransactionRequestDto) =
        TransactionDto(
            id = 1,
            accountId = request.accountId,
            categoryId = request.categoryId,
            amount = request.amount,
            transactionDate = request.transactionDate,
            comment = request.comment,
            createdAt = request.transactionDate,
            updatedAt = request.transactionDate,
        )

    override suspend fun updateTransaction(
        id: Int,
        request: com.yandex.school.casheye.data.finance.dto.TransactionRequestDto,
    ) = transactionDto(id, request.amount, request.transactionDate)

    override suspend fun deleteTransaction(id: Int) = Unit

    override suspend fun getTransactions(
        accountId: Int,
        startDate: String,
        endDate: String,
    ): List<TransactionResponseDto> {
        requests += PeriodRequest(accountId, startDate, endDate)
        if (accountId == failingAccountId) {
            throw IOException("Account request failed")
        }
        return transactionsByAccount[accountId].orEmpty()
    }
}

private class ThrowingFinanceApi(
    private val error: Exception,
) : FinanceApi {
    var accountRequests: Int = 0

    override suspend fun getAccounts(): List<AccountDto> {
        accountRequests += 1
        throw error
    }

    override suspend fun getAccount(id: Int) = throw error

    override suspend fun createAccount(request: com.yandex.school.casheye.data.finance.dto.AccountRequestDto) =
        throw error

    override suspend fun updateAccount(
        id: Int,
        request: com.yandex.school.casheye.data.finance.dto.AccountRequestDto,
    ) = throw error

    override suspend fun deleteAccount(id: Int) = throw error

    override suspend fun getCategories(isIncome: Boolean) = throw error

    override suspend fun getTransaction(id: Int) = throw error

    override suspend fun createTransaction(request: com.yandex.school.casheye.data.finance.dto.TransactionRequestDto) =
        throw error

    override suspend fun updateTransaction(
        id: Int,
        request: com.yandex.school.casheye.data.finance.dto.TransactionRequestDto,
    ) = throw error

    override suspend fun deleteTransaction(id: Int) = throw error

    override suspend fun getTransactions(
        accountId: Int,
        startDate: String,
        endDate: String,
    ): List<TransactionResponseDto> = throw AssertionError("Unexpected transaction request")
}

private fun accountDto(id: Int): AccountDto =
    AccountDto(
        id = id,
        userId = 1,
        name = "Счёт $id",
        emoji = "\uD83D\uDCB5",
        balance = "1000.00",
        currency = "RUB",
        createdAt = Instant.parse("2026-07-17T00:00:00Z"),
        updatedAt = Instant.parse("2026-07-17T00:00:00Z"),
    )

private fun transactionDto(
    id: Int,
    amount: String,
    transactionDate: Instant,
    isIncome: Boolean = false,
): TransactionResponseDto =
    TransactionResponseDto(
        id = id,
        account = AccountBriefDto(1, "Основной счёт", "💵", "1000.00", "RUB"),
        category = CategoryDto(id, "Категория $id", "🛒", isIncome),
        amount = amount,
        transactionDate = transactionDate,
        comment = null,
        createdAt = transactionDate,
        updatedAt = transactionDate,
    )
