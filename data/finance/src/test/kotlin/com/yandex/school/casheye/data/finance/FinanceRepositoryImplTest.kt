package com.yandex.school.casheye.data.finance

import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.dto.AccountBriefDto
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.data.finance.repository.FinanceRepositoryImpl
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
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
    fun `requests every account with same current-day boundaries`() =
        runBlocking {
            val api =
                FakeFinanceApi(
                    accounts = listOf(accountDto(1), accountDto(2), accountDto(3)),
                    transactionsByAccount = emptyMap(),
                )
            val date = LocalDate.of(2026, 7, 17)

            FinanceRepositoryImpl(api, Dispatchers.Unconfined).getDailySummary(date, "RUB", TransactionKind.Expense)

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
                FinanceRepositoryImpl(api, Dispatchers.Unconfined).getDailySummary(
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
                FinanceRepositoryImpl(api, Dispatchers.Unconfined).getDailySummary(
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
                FinanceRepositoryImpl(api, Dispatchers.Unconfined).getDailySummary(
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
                FinanceRepositoryImpl(api, Dispatchers.Unconfined).getDailySummary(
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
                FinanceRepositoryImpl(api, Dispatchers.Unconfined).getDailySummary(
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
                FinanceRepositoryImpl(api, Dispatchers.Unconfined).getDailySummary(
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
                FinanceRepositoryImpl(api, Dispatchers.Unconfined).getDailySummary(
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
