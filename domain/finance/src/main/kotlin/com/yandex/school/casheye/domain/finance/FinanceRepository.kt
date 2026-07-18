package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate

interface FinanceRepository {
    suspend fun getDailySummary(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult

    suspend fun getAccountsSummary(currencyCode: String): AccountsLoadResult
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
