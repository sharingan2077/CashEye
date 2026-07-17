package com.yandex.school.casheye.domain.expenses

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate

interface ExpensesRepository {
    suspend fun getExpenses(
        date: LocalDate,
        currencyCode: String,
    ): ExpensesLoadResult
}

data class ExpensesSummary(
    val total: BigDecimal,
    val currencyCode: String,
    val transactions: List<Transaction>,
)

sealed interface ExpensesLoadResult {
    data class Success(
        val summary: ExpensesSummary,
    ) : ExpensesLoadResult

    data class Failure(
        val reason: ExpensesFailureReason,
    ) : ExpensesLoadResult
}

sealed interface ExpensesFailureReason {
    data object Network : ExpensesFailureReason

    data object Authorization : ExpensesFailureReason

    data object Server : ExpensesFailureReason

    data object Unknown : ExpensesFailureReason
}
