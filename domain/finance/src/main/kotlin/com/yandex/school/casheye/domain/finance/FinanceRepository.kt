package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate

interface FinanceRepository {
    suspend fun getAccounts(): FinanceDataLoadResult<List<Account>>

    suspend fun getTransactions(query: TransactionsQuery): FinanceDataLoadResult<List<Transaction>>

    suspend fun getCategories(isIncome: Boolean): EditorResult<List<Category>> =
        EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun getTransaction(id: Int): EditorResult<Transaction> = EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun saveTransaction(command: SaveTransactionCommand): EditorResult<Unit> =
        EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun getAccount(id: Int): EditorResult<Account> = EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun saveAccount(command: SaveAccountCommand): EditorResult<Unit> =
        EditorResult.Failure(FinanceFailureReason.Unknown)
}

data class TransactionsQuery(
    val accountIds: Set<Int>,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

sealed interface FinanceDataLoadResult<out T> {
    data class Success<T>(
        val data: T,
    ) : FinanceDataLoadResult<T>

    data class Failure(
        val reason: FinanceFailureReason,
    ) : FinanceDataLoadResult<Nothing>
}

data class AnalyticsQuery(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val currencyCode: String,
    val transactionKind: AnalyticsTransactionKind,
    val accountId: Int?,
    val categoryIds: Set<Int>,
)

enum class AnalyticsTransactionKind {
    Income,
    Expense,
    All,
}

data class AnalyticsSummary(
    val total: BigDecimal,
    val currencyCode: String,
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val availableCategories: List<Category>,
)

sealed interface AnalyticsLoadResult {
    data class Success(
        val summary: AnalyticsSummary,
    ) : AnalyticsLoadResult

    data class Failure(
        val reason: FinanceFailureReason,
    ) : AnalyticsLoadResult
}

data class AccountsSummary(
    val total: BigDecimal,
    val currencyCode: String,
    val accounts: List<Account>,
)

sealed interface AccountsLoadResult {
    data class Success(
        val summary: AccountsSummary,
    ) : AccountsLoadResult

    data class Failure(
        val reason: FinanceFailureReason,
    ) : AccountsLoadResult
}

enum class TransactionKind {
    Income,
    Expense,
}

data class FinanceSummary(
    val total: BigDecimal,
    val currencyCode: String,
    val transactions: List<Transaction>,
)

sealed interface FinanceLoadResult {
    data class Success(
        val summary: FinanceSummary,
    ) : FinanceLoadResult

    data class Failure(
        val reason: FinanceFailureReason,
    ) : FinanceLoadResult
}

sealed interface FinanceFailureReason {
    data object Network : FinanceFailureReason

    data object Authorization : FinanceFailureReason

    data object Server : FinanceFailureReason

    data object Unknown : FinanceFailureReason
}
