package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceLocalStore
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.data.finance.network.ServerRetryPolicy
import com.yandex.school.casheye.data.finance.sync.FinanceSyncScheduler
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceDataLoadResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.SaveTransactionCommand
import com.yandex.school.casheye.domain.finance.TransactionsQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FinanceRepositoryImpl(
    private val api: FinanceApi,
    private val localStore: FinanceLocalStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val syncScheduler: FinanceSyncScheduler = NoOpFinanceSyncScheduler,
    waitBeforeRetry: suspend (Long) -> Unit = { delay(it) },
) : FinanceRepository {
    private val retryPolicy = ServerRetryPolicy(waitBeforeRetry)

    override fun observeAccounts(): Flow<List<Account>> = localStore.observeAccounts()

    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> {
        val period = query.startDate.toPeriod(query.endDate)
        return localStore
            .observeTransactions(null, period.start, period.end)
            .map { transactions ->
                if (query.accountIds.isEmpty()) {
                    transactions
                } else {
                    transactions.filter { it.account.id in query.accountIds }
                }
            }
    }

    override suspend fun refreshAccounts(): FinanceRefreshResult =
        refreshCatching {
            localStore.refreshAccounts(retryPolicy.execute { api.getAccounts() })
        }

    override suspend fun refreshPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
    ): FinanceRefreshResult =
        refreshCatching {
            loadRemotePeriod(startDate.toPeriod(endDate))
        }

    override suspend fun getAccounts(): FinanceDataLoadResult<List<Account>> =
        withContext(ioDispatcher) {
            val refreshFailure =
                try {
                    localStore.refreshAccounts(retryPolicy.execute { api.getAccounts() })
                    null
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    error.toFailureReason()
                }
            try {
                val accounts = localStore.getAccounts()
                if (refreshFailure != null && accounts.isEmpty()) {
                    FinanceDataLoadResult.Failure(refreshFailure)
                } else {
                    FinanceDataLoadResult.Success(accounts)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                FinanceDataLoadResult.Failure(FinanceFailureReason.Unknown)
            }
        }

    override suspend fun getCategories(isIncome: Boolean): EditorResult<List<Category>> =
        localFirstEditorRequest(
            refresh = { localStore.refreshCategories(retryPolicy.execute { api.getCategories(isIncome) }) },
            read = { localStore.getCategories(isIncome) },
            hasCache = { it.isNotEmpty() },
        )

    override suspend fun getTransaction(id: Int): EditorResult<Transaction> =
        editorRequest(ioDispatcher) {
            localStore.getTransaction(id)
                ?: retryPolicy.execute { api.getTransaction(id) }.also { localStore.cacheTransaction(it) }.let {
                    checkNotNull(localStore.getTransaction(it.id))
                }
        }

    override suspend fun saveTransaction(command: SaveTransactionCommand): EditorResult<Unit> =
        editorRequest(ioDispatcher) {
            localStore.saveTransaction(command, Instant.now())
            scheduleSync()
        }

    override suspend fun deleteTransaction(id: Int): EditorResult<Unit> =
        editorRequest(ioDispatcher) {
            localStore.deleteTransaction(id, Instant.now())
            scheduleSync()
        }

    override suspend fun getAccount(id: Int): EditorResult<Account> =
        editorRequest(ioDispatcher) {
            localStore.getAccount(id)
                ?: retryPolicy.execute { api.getAccount(id) }.also { localStore.cacheAccount(it) }.let {
                    checkNotNull(localStore.getAccount(it.id))
                }
        }

    override suspend fun saveAccount(command: SaveAccountCommand): EditorResult<Unit> =
        editorRequest(ioDispatcher) {
            localStore.saveAccount(command, Instant.now())
            scheduleSync()
        }

    override suspend fun getAccountTransactionCount(id: Int): EditorResult<Int> =
        editorRequest(ioDispatcher) {
            if (id > 0) {
                try {
                    retryPolicy
                        .execute {
                            api.getTransactions(
                                accountId = id,
                                startDate = EARLIEST_TRANSACTION_DATE,
                                endDate = LocalDate.now().toString(),
                            )
                        }.forEach { localStore.cacheTransaction(it) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Offline deletion uses the fully preloaded local history.
                }
            }
            localStore.getAccountTransactionCount(id)
        }

    override suspend fun deleteAccount(id: Int): EditorResult<Int> =
        editorRequest(ioDispatcher) {
            localStore.deleteAccount(id, Instant.now()).also { scheduleSync() }
        }

    override suspend fun getTransactions(query: TransactionsQuery): FinanceDataLoadResult<List<Transaction>> =
        withContext(ioDispatcher) {
            val period = query.startDate.toPeriod(query.endDate)
            val refreshFailure =
                when (val refresh = refreshPeriod(query.startDate, query.endDate)) {
                    FinanceRefreshResult.Success -> null
                    is FinanceRefreshResult.Failure -> refresh.reason
                }
            try {
                val accounts = localStore.getAccounts()
                val transactions =
                    localStore
                        .getTransactions(null, period.start, period.end)
                        .filter { it.account.id in query.accountIds }
                if (refreshFailure != null && transactions.isEmpty() && accounts.isEmpty()) {
                    FinanceDataLoadResult.Failure(refreshFailure)
                } else {
                    FinanceDataLoadResult.Success(transactions)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                FinanceDataLoadResult.Failure(FinanceFailureReason.Unknown)
            }
        }

    private suspend fun refreshCatching(block: suspend () -> Unit): FinanceRefreshResult =
        try {
            withContext(ioDispatcher) { block() }
            FinanceRefreshResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            FinanceRefreshResult.Failure(
                reason = error.toFailureReason(),
                hasUsableCache = hasUsableCache(),
            )
        }

    private suspend fun hasUsableCache(): Boolean =
        try {
            withContext(ioDispatcher) { localStore.hasUsableCache() }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }

    private suspend fun loadRemotePeriod(period: InstantPeriod) {
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
        end =
            endDate
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .minusMillis(1),
    )
}

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
        is IOException -> {
            FinanceFailureReason.Network
        }

        is HttpException -> {
            when (code()) {
                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> FinanceFailureReason.Authorization
                in HttpStatus.SERVER_ERROR_MIN..HttpStatus.SERVER_ERROR_MAX -> FinanceFailureReason.Server
                else -> FinanceFailureReason.Unknown
            }
        }

        else -> {
            FinanceFailureReason.Unknown
        }
    }

private object HttpStatus {
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val SERVER_ERROR_MIN = 500
    const val SERVER_ERROR_MAX = 599
}

private const val EARLIEST_TRANSACTION_DATE = "1970-01-01"
