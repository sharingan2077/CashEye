package com.yandex.school.casheye.data.expenses

import com.yandex.school.casheye.data.expenses.api.ExpensesApi
import com.yandex.school.casheye.data.expenses.dto.AccountBriefDto
import com.yandex.school.casheye.data.expenses.dto.AccountDto
import com.yandex.school.casheye.data.expenses.dto.CategoryDto
import com.yandex.school.casheye.data.expenses.dto.TransactionResponseDto
import com.yandex.school.casheye.data.expenses.repository.ExpensesRepositoryImpl
import com.yandex.school.casheye.domain.expenses.ExpensesFailureReason
import com.yandex.school.casheye.domain.expenses.ExpensesLoadResult
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
import kotlin.collections.mapOf
import kotlin.to

class ExpensesRepositoryImplTest {
    @Test
    fun `requests every account with same current-day boundaries`() =
        runBlocking {
            val api =
                FakeExpensesApi(
                    accounts = listOf(accountDto(1), accountDto(2), accountDto(3)),
                    transactionsByAccount = emptyMap(),
                )
            val date = LocalDate.of(2026, 7, 17)

            ExpensesRepositoryImpl(api, Dispatchers.Unconfined).getExpenses(date, "RUB")

            assertEquals(setOf(1, 2, 3), api.requests.map { it.accountId }.toSet())
            assertTrue(api.requests.all { it.startDate == "2026-07-17" })
            assertTrue(api.requests.all { it.endDate == "2026-07-17" })
        }

    @Test
    fun `filters income sorts newest first and calculates total`() =
        runBlocking {
            val api =
                FakeExpensesApi(
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
                ExpensesRepositoryImpl(api, Dispatchers.Unconfined).getExpenses(
                    date = LocalDate.of(2026, 7, 17),
                    currencyCode = "RUB",
                ) as ExpensesLoadResult.Success

            assertEquals(listOf(3, 1), result.summary.transactions.map { it.id })
            assertEquals(BigDecimal("31.00"), result.summary.total)
            assertEquals("RUB", result.summary.currencyCode)
        }

    @Test
    fun `no accounts returns successful empty summary`() =
        runBlocking {
            val api = FakeExpensesApi(accounts = emptyList(), transactionsByAccount = emptyMap())

            val result =
                ExpensesRepositoryImpl(api, Dispatchers.Unconfined).getExpenses(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                ) as ExpensesLoadResult.Success

            assertTrue(result.summary.transactions.isEmpty())
            assertEquals(BigDecimal.ZERO, result.summary.total)
            assertTrue(api.requests.isEmpty())
        }

    @Test
    fun `only income returns successful empty summary`() =
        runBlocking {
            val api =
                FakeExpensesApi(
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
                ExpensesRepositoryImpl(api, Dispatchers.Unconfined).getExpenses(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                ) as ExpensesLoadResult.Success

            assertTrue(result.summary.transactions.isEmpty())
            assertEquals(BigDecimal.ZERO, result.summary.total)
        }

    @Test
    fun `network exception maps to network failure`() =
        runBlocking {
            val api = ThrowingExpensesApi(IOException("offline"))

            val result =
                ExpensesRepositoryImpl(api, Dispatchers.Unconfined).getExpenses(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                )

            assertEquals(
                ExpensesLoadResult.Failure(ExpensesFailureReason.Network),
                result,
            )
        }

    @Test
    fun `server response maps to server failure`() =
        runBlocking {
            val response = Response.error<Unit>(500, "server error".toResponseBody())
            val api = ThrowingExpensesApi(HttpException(response))

            val result =
                ExpensesRepositoryImpl(api, Dispatchers.Unconfined).getExpenses(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                )

            assertEquals(
                ExpensesLoadResult.Failure(ExpensesFailureReason.Server),
                result,
            )
        }

    @Test
    fun `one failed account request fails the whole load`() =
        runBlocking {
            val api =
                FakeExpensesApi(
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
                ExpensesRepositoryImpl(api, Dispatchers.Unconfined).getExpenses(
                    LocalDate.of(2026, 7, 17),
                    "RUB",
                )

            assertEquals(
                ExpensesLoadResult.Failure(ExpensesFailureReason.Network),
                result,
            )
        }
}

private data class PeriodRequest(
    val accountId: Int,
    val startDate: String,
    val endDate: String,
)

private class FakeExpensesApi(
    private val accounts: List<AccountDto>,
    private val transactionsByAccount: Map<Int, List<TransactionResponseDto>>,
    private val failingAccountId: Int? = null,
) : ExpensesApi {
    val requests: MutableList<PeriodRequest> = Collections.synchronizedList(mutableListOf())

    override suspend fun getAccounts(): List<AccountDto> = accounts

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

private class ThrowingExpensesApi(
    private val error: Exception,
) : ExpensesApi {
    override suspend fun getAccounts(): List<AccountDto> = throw error

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
