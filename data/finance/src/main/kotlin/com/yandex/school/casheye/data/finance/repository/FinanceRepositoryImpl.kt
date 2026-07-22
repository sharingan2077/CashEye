package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceLocalStore
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.data.finance.sync.FinanceSyncScheduler
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.AccountsSummary
import com.yandex.school.casheye.domain.finance.AnalyticsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsQuery
import com.yandex.school.casheye.domain.finance.AnalyticsSummary
import com.yandex.school.casheye.domain.finance.AnalyticsTransactionKind
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.FinanceSummary
import com.yandex.school.casheye.domain.finance.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.SaveTransactionCommand
import com.yandex.school.casheye.domain.finance.TransactionKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FinanceRepositoryImpl(
    private val api: FinanceApi,
    private val localStore: FinanceLocalStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val syncScheduler: FinanceSyncScheduler = NoOpFinanceSyncScheduler,
) : FinanceRepository {
    override suspend fun getAccounts(): EditorResult<List<Account>> =
        localFirstEditorRequest(
            refresh = { localStore.refreshAccounts(api.getAccounts()) },
            read = { localStore.getAccounts() },
            hasCache = { it.isNotEmpty() },
        )

    override suspend fun getCategories(isIncome: Boolean): EditorResult<List<Category>> =
        localFirstEditorRequest(
            refresh = { localStore.refreshCategories(api.getCategories(isIncome)) },
            read = { localStore.getCategories(isIncome) },
            hasCache = { it.isNotEmpty() },
        )

    override suspend fun getTransaction(id: Int): EditorResult<Transaction> =
        editorRequest(ioDispatcher) {
            localStore.getTransaction(id)
                ?: api.getTransaction(id).also { localStore.cacheTransaction(it) }.let {
                    checkNotNull(localStore.getTransaction(it.id))
                }
        }

    override suspend fun saveTransaction(command: SaveTransactionCommand): EditorResult<Unit> =
        editorRequest(ioDispatcher) {
            localStore.saveTransaction(command, Instant.now())
            scheduleSync()
        }

    override suspend fun getAccount(id: Int): EditorResult<Account> =
        editorRequest(ioDispatcher) {
            localStore.getAccount(id)
                ?: api.getAccount(id).also { localStore.cacheAccount(it) }.let {
                    checkNotNull(localStore.getAccount(it.id))
                }
        }

    override suspend fun saveAccount(command: SaveAccountCommand): EditorResult<Unit> =
        editorRequest(ioDispatcher) {
            localStore.saveAccount(command, Instant.now())
            scheduleSync()
        }

    override suspend fun getAnalytics(query: AnalyticsQuery): AnalyticsLoadResult =
        withContext(ioDispatcher) {
            val period = query.startDate.toPeriod(query.endDate)
            val refreshFailure = refreshPeriodCatching(period)
            try {
                val accounts = localStore.getAccounts()
                val transactions = localStore.getTransactions(query.accountId, period.start, period.end)
                if (refreshFailure != null && transactions.isEmpty() && accounts.isEmpty()) {
                    return@withContext AnalyticsLoadResult.Failure(refreshFailure)
                }
                val kindFiltered = transactions.filterBy(query.transactionKind)
                val availableCategories =
                    kindFiltered.map { it.category }.distinctBy { it.id }.sortedBy { it.name }
                val filtered =
                    kindFiltered
                        .filter { query.categoryIds.isEmpty() || it.category.id in query.categoryIds }
                        .sortedByDescending { it.transactionDate }
                AnalyticsLoadResult.Success(
                    AnalyticsSummary(
                        total = filtered.sumAmounts(),
                        currencyCode = query.currencyCode,
                        transactions = filtered,
                        accounts = accounts,
                        availableCategories = availableCategories,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                AnalyticsLoadResult.Failure(FinanceFailureReason.Unknown)
            }
        }

    override suspend fun getDailySummary(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult =
        withContext(ioDispatcher) {
            val period = date.toPeriod(date)
            val refreshFailure = refreshPeriodCatching(period)
            try {
                val accounts = localStore.getAccounts()
                val transactions =
                    localStore.getTransactions(null, period.start, period.end)
                        .filter {
                            when (transactionKind) {
                                TransactionKind.Income -> it.category.isIncome
                                TransactionKind.Expense -> !it.category.isIncome
                            }
                        }.sortedByDescending { it.transactionDate }
                if (refreshFailure != null && transactions.isEmpty() && accounts.isEmpty()) {
                    return@withContext FinanceLoadResult.Failure(refreshFailure)
                }
                FinanceLoadResult.Success(
                    FinanceSummary(
                        total = transactions.sumAmounts(),
                        currencyCode = currencyCode,
                        transactions = transactions,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                FinanceLoadResult.Failure(FinanceFailureReason.Unknown)
            }
        }

    override suspend fun getAccountsSummary(currencyCode: String): AccountsLoadResult =
        withContext(ioDispatcher) {
            val refreshFailure =
                try {
                    localStore.refreshAccounts(api.getAccounts())
                    null
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    error.toFailureReason()
                }
            try {
                val accounts = localStore.getAccounts()
                if (refreshFailure != null && accounts.isEmpty()) {
                    return@withContext AccountsLoadResult.Failure(refreshFailure)
                }
                AccountsLoadResult.Success(
                    AccountsSummary(
                        total = accounts.fold(BigDecimal.ZERO) { total, account -> total + account.balance },
                        currencyCode = currencyCode,
                        accounts = accounts,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                AccountsLoadResult.Failure(FinanceFailureReason.Unknown)
            }
        }

    private suspend fun refreshPeriodCatching(period: InstantPeriod): FinanceFailureReason? =
        try {
            refreshPeriod(period)
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            error.toFailureReason()
        }

    private suspend fun refreshPeriod(period: InstantPeriod) {
        val requestStart = period.startDate.toString()
        val requestEnd = period.endDate.toString()
        val snapshot =
            coroutineScope {
                val accounts = async { api.getAccounts() }
                val incomeCategories = async { api.getCategories(true) }
                val expenseCategories = async { api.getCategories(false) }
                val loadedAccounts = accounts.await()
                val transactions =
                    loadedAccounts.map { account ->
                        async { api.getTransactions(account.id, requestStart, requestEnd) }
                    }.awaitAll().flatten()
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

    private fun scheduleSync() {
        runCatching(syncScheduler::enqueueImmediateSync)
    }

    private suspend fun <T> localFirstEditorRequest(
        refresh: suspend () -> Unit,
        read: suspend () -> T,
        hasCache: (T) -> Boolean,
    ): EditorResult<T> =
        try {
            withContext(ioDispatcher) {
                val refreshFailure =
                    try {
                        refresh()
                        null
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        error.toFailureReason()
                    }
                val local = read()
                if (refreshFailure == null || hasCache(local)) {
                    EditorResult.Success(local)
                } else {
                    EditorResult.Failure(refreshFailure)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            EditorResult.Failure(FinanceFailureReason.Unknown)
        }
}

private data object NoOpFinanceSyncScheduler : FinanceSyncScheduler {
    override fun registerPeriodicSync() = Unit

    override fun enqueueImmediateSync() = Unit
}

private data class RemoteSnapshot(
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionResponseDto>,
)

private data class InstantPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val start: Instant,
    val end: Instant,
)

private fun LocalDate.toPeriod(endDate: LocalDate): InstantPeriod {
    val zone = ZoneId.systemDefault()
    return InstantPeriod(
        startDate = this,
        endDate = endDate,
        start = atStartOfDay(zone).toInstant(),
        end = endDate.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1),
    )
}

private fun List<Transaction>.filterBy(kind: AnalyticsTransactionKind): List<Transaction> =
    filter {
        when (kind) {
            AnalyticsTransactionKind.Income -> it.category.isIncome
            AnalyticsTransactionKind.Expense -> !it.category.isIncome
            AnalyticsTransactionKind.All -> true
        }
    }

private fun List<Transaction>.sumAmounts(): BigDecimal =
    fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount }

private suspend inline fun <T> editorRequest(
    dispatcher: CoroutineDispatcher,
    crossinline block: suspend () -> T,
): EditorResult<T> =
    try {
        EditorResult.Success(withContext(dispatcher) { block() })
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        EditorResult.Failure(error.toFailureReason())
    }

private fun Exception.toFailureReason(): FinanceFailureReason =
    when (this) {
        is IOException -> FinanceFailureReason.Network
        is HttpException ->
            when (code()) {
                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> FinanceFailureReason.Authorization
                in HttpStatus.SERVER_ERROR_MIN..HttpStatus.SERVER_ERROR_MAX -> FinanceFailureReason.Server
                else -> FinanceFailureReason.Unknown
            }
        else -> FinanceFailureReason.Unknown
    }

private object HttpStatus {
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val SERVER_ERROR_MIN = 500
    const val SERVER_ERROR_MAX = 599
}
