package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceLocalStore
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.data.finance.network.ServerRetryPolicy
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Downloads a consistent finance snapshot for one period before replacing its cached range. */
internal class FinancePeriodRefresher(
    private val api: FinanceApi,
    private val localStore: FinanceLocalStore,
    private val retryPolicy: ServerRetryPolicy,
) {
    suspend fun refresh(period: FinancePeriod) {
        val requestStart = period.startDate.toString()
        val requestEnd = period.endDate.toString()
        val snapshot =
            coroutineScope {
                val accounts = async { retryPolicy.execute { api.getAccounts() } }
                val incomeCategories = async { retryPolicy.execute { api.getCategories(true) } }
                val expenseCategories = async { retryPolicy.execute { api.getCategories(false) } }
                val loadedAccounts = accounts.await()
                val transactions =
                    loadedAccounts
                        .map { account ->
                            async {
                                retryPolicy.execute {
                                    api.getTransactions(account.id, requestStart, requestEnd)
                                }
                            }
                        }.awaitAll()
                        .flatten()
                RemoteSnapshot(
                    accounts = loadedAccounts,
                    categories = incomeCategories.await() + expenseCategories.await(),
                    transactions = transactions,
                )
            }
        localStore.refreshPeriod(
            accounts = snapshot.accounts,
            categories = snapshot.categories,
            transactions = snapshot.transactions,
            startInclusive = period.start,
            endInclusive = period.end,
        )
    }
}

private data class RemoteSnapshot(
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionResponseDto>,
)
