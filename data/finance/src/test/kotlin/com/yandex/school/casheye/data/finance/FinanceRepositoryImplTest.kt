package com.yandex.school.casheye.data.finance

import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceLocalStore
import com.yandex.school.casheye.data.finance.dto.AccountBriefDto
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionRequestDto
import com.yandex.school.casheye.data.finance.dto.TransactionDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.data.finance.mapper.toDomain
import com.yandex.school.casheye.data.finance.repository.FinanceRepositoryImpl
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.SaveTransactionCommand
import com.yandex.school.casheye.domain.finance.TransactionKind
import kotlinx.coroutines.Dispatchers
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
    fun `editor maps io exception to network failure`() =
        runBlocking {
            val result =
                FinanceRepositoryImpl(
                    ThrowingFinanceApi(IOException("offline")),
                    FakeFinanceLocalStore(),
                    Dispatchers.Unconfined,
                ).getAccounts()

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

            repository(api).getDailySummary(date, "RUB", TransactionKind.Expense)

            assertEquals(setOf(1, 2, 3), api.requests.map { it.accountId }.toSet())
            assertTrue(api.requests.all { it.startDate == "2026-07-17" })
            assertTrue(api.requests.all { it.endDate == "2026-07-17" })
        }

    @Test
    fun `filters expenses sorts newest first and calculates total`() =
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
                repository(api).getDailySummary(
                    date = LocalDate.of(2026, 7, 17),
                    currencyCode = "RUB",
                    transactionKind = TransactionKind.Expense,
                ) as FinanceLoadResult.Success

            assertEquals(listOf(3, 1), result.summary.transactions.map { it.id })
            assertEquals(BigDecimal("31.00"), result.summary.total)
            assertEquals("RUB", result.summary.currencyCode)
        }

    @Test
    fun `no accounts returns successful empty summary`() =
        runBlocking {
            val api = FakeFinanceApi(accounts = emptyList(), transactionsByAccount = emptyMap())

            val result =
                repository(api).getDailySummary(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                    TransactionKind.Expense,
                ) as FinanceLoadResult.Success

            assertTrue(result.summary.transactions.isEmpty())
            assertEquals(BigDecimal.ZERO, result.summary.total)
            assertTrue(api.requests.isEmpty())
        }

    @Test
    fun `only income returns successful empty summary`() =
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
                repository(api).getDailySummary(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                    TransactionKind.Expense,
                ) as FinanceLoadResult.Success

            assertTrue(result.summary.transactions.isEmpty())
            assertEquals(BigDecimal.ZERO, result.summary.total)
        }

    @Test
    fun `income kind keeps only income transactions`() =
        runBlocking {
            val api =
                FakeFinanceApi(
                    accounts = listOf(accountDto(1)),
                    transactionsByAccount =
                        mapOf(
                            1 to
                                listOf(
                                    transactionDto(1, "100.00", Instant.parse("2026-07-17T10:00:00Z"), true),
                                    transactionDto(2, "20.00", Instant.parse("2026-07-17T11:00:00Z")),
                                ),
                        ),
                )

            val result =
                repository(api).getDailySummary(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                    TransactionKind.Income,
                ) as FinanceLoadResult.Success

            assertEquals(listOf(1), result.summary.transactions.map { it.id })
            assertEquals(BigDecimal("100.00"), result.summary.total)
        }

    @Test
    fun `network exception maps to network failure`() =
        runBlocking {
            val api = ThrowingFinanceApi(IOException("offline"))

            val result =
                repository(api).getDailySummary(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                    TransactionKind.Expense,
                )

            assertEquals(
                FinanceLoadResult.Failure(FinanceFailureReason.Network),
                result,
            )
        }

    @Test
    fun `server response maps to server failure`() =
        runBlocking {
            val response = Response.error<Unit>(500, "server error".toResponseBody())
            val api = ThrowingFinanceApi(HttpException(response))

            val result =
                repository(api).getDailySummary(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                    TransactionKind.Expense,
                )

            assertEquals(
                FinanceLoadResult.Failure(FinanceFailureReason.Server),
                result,
            )
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

            val result =
                repository(api).getDailySummary(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                    TransactionKind.Expense,
                )

            assertEquals(
                FinanceLoadResult.Failure(FinanceFailureReason.Network),
                result,
            )
        }
}

private fun repository(
    api: FinanceApi,
    localStore: FinanceLocalStore = FakeFinanceLocalStore(),
): FinanceRepositoryImpl = FinanceRepositoryImpl(api, localStore, Dispatchers.Unconfined)

private class FakeFinanceLocalStore : FinanceLocalStore {
    private var accounts: List<Account> = emptyList()
    private var categories: List<Category> = emptyList()
    private var transactions: List<Transaction> = emptyList()
    var savedTransaction: SaveTransactionCommand? = null

    override suspend fun getAccounts(): List<Account> = accounts

    override suspend fun getAccount(id: Int): Account? = accounts.firstOrNull { it.id == id }

    override suspend fun getCategories(isIncome: Boolean): List<Category> =
        categories.filter { it.isIncome == isIncome }

    override suspend fun getTransaction(id: Int): Transaction? = transactions.firstOrNull { it.id == id }

    override suspend fun getTransactions(
        accountId: Int?,
        startInclusive: Instant,
        endInclusive: Instant,
    ): List<Transaction> =
        transactions.filter {
            (accountId == null || it.account.id == accountId) &&
                it.transactionDate >= startInclusive &&
                it.transactionDate <= endInclusive
        }

    override suspend fun refreshAccounts(accounts: List<AccountDto>) {
        this.accounts = accounts.map { it.toDomain() }
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
        this.accounts = accounts.map { it.toDomain() }
        this.categories = categories.map { it.toDomain() }
        this.transactions = transactions.map { it.toDomain() }
    }

    override suspend fun cacheAccount(account: com.yandex.school.casheye.data.finance.dto.AccountResponseDto) {
        accounts = accounts.filterNot { it.id == account.id } + account.toDomain()
    }

    override suspend fun cacheTransaction(transaction: TransactionResponseDto) {
        transactions = transactions.filterNot { it.id == transaction.id } + transaction.toDomain()
    }

    override suspend fun saveAccount(
        command: com.yandex.school.casheye.domain.finance.SaveAccountCommand,
        now: Instant,
    ) = Unit

    override suspend fun saveTransaction(command: SaveTransactionCommand, now: Instant) {
        savedTransaction = command
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
    override suspend fun getAccounts(): List<AccountDto> = throw error

    override suspend fun getAccount(id: Int) = throw error

    override suspend fun createAccount(request: com.yandex.school.casheye.data.finance.dto.AccountRequestDto) =
        throw error

    override suspend fun updateAccount(
        id: Int,
        request: com.yandex.school.casheye.data.finance.dto.AccountRequestDto,
    ) = throw error

    override suspend fun getCategories(isIncome: Boolean) = throw error

    override suspend fun getTransaction(id: Int) = throw error

    override suspend fun createTransaction(request: com.yandex.school.casheye.data.finance.dto.TransactionRequestDto) =
        throw error

    override suspend fun updateTransaction(
        id: Int,
        request: com.yandex.school.casheye.data.finance.dto.TransactionRequestDto,
    ) = throw error

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
