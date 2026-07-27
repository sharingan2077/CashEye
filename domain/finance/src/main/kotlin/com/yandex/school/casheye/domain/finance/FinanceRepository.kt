package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.editor.EditorResult
import com.yandex.school.casheye.domain.finance.editor.AccountCurrencyChangeEligibility
import com.yandex.school.casheye.domain.finance.editor.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.editor.SaveTransactionCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

interface FinanceQueryRepository {
    fun observeAccounts(): Flow<List<Account>> = flowOf(emptyList())

    fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> = flowOf(emptyList())

    suspend fun refreshAccounts(): FinanceRefreshResult =
        FinanceRefreshResult.Failure(FinanceFailureReason.Unknown, hasUsableCache = false)

    suspend fun refreshPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
    ): FinanceRefreshResult = FinanceRefreshResult.Failure(FinanceFailureReason.Unknown, hasUsableCache = false)

    suspend fun getAccounts(): FinanceDataLoadResult<List<Account>>

    suspend fun getTransactions(query: TransactionsQuery): FinanceDataLoadResult<List<Transaction>>
}

interface FinanceEditorRepository {
    suspend fun getCategories(isIncome: Boolean): EditorResult<List<Category>> =
        EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun getTransaction(id: Int): EditorResult<Transaction> = EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun saveTransaction(command: SaveTransactionCommand): EditorResult<Unit> =
        EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun deleteTransaction(id: Int): EditorResult<Unit> = EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun getAccount(id: Int): EditorResult<Account> = EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun saveAccount(command: SaveAccountCommand): EditorResult<Unit> =
        EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun getAccountCurrencyChangeEligibility(id: Int): EditorResult<AccountCurrencyChangeEligibility> =
        EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun getAccountTransactionCount(id: Int): EditorResult<Int> =
        EditorResult.Failure(FinanceFailureReason.Unknown)

    suspend fun deleteAccount(id: Int): EditorResult<Int> = EditorResult.Failure(FinanceFailureReason.Unknown)
}

interface FinanceRepository :
    FinanceQueryRepository,
    FinanceEditorRepository

data class TransactionsQuery(
    val accountIds: Set<Int>,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
