package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.dto.AccountRequestDto
import com.yandex.school.casheye.data.finance.dto.TransactionRequestDto
import com.yandex.school.casheye.data.finance.mapper.toDomain
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
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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
import java.time.LocalDate

@Inject
@SingleIn(AppScope::class)
class FinanceRepositoryImpl(
    private val api: FinanceApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FinanceRepository {
    override suspend fun getAccounts() = editorRequest(ioDispatcher) { api.getAccounts().map { it.toDomain() } }

    override suspend fun getCategories(isIncome: Boolean) =
        editorRequest(ioDispatcher) { api.getCategories(isIncome).map { it.toDomain() } }

    override suspend fun getTransaction(id: Int) = editorRequest(ioDispatcher) { api.getTransaction(id).toDomain() }

    override suspend fun saveTransaction(command: SaveTransactionCommand): EditorResult<Unit> =
        editorRequest(ioDispatcher) {
            val request =
                TransactionRequestDto(
                    accountId = command.accountId,
                    categoryId = command.categoryId,
                    amount = command.amount.toPlainString(),
                    transactionDate = command.transactionDate,
                    comment = command.comment,
                )
            command.id?.let { api.updateTransaction(it, request) } ?: api.createTransaction(request)
            Unit
        }

    override suspend fun getAccount(id: Int) = editorRequest(ioDispatcher) { api.getAccount(id).toDomain() }

    override suspend fun saveAccount(command: SaveAccountCommand): EditorResult<Unit> =
        editorRequest(ioDispatcher) {
            val request =
                AccountRequestDto(
                    name = command.name,
                    emoji = command.emoji,
                    balance = command.balance.toPlainString(),
                    currency = command.currency,
                )
            command.id?.let { api.updateAccount(it, request) } ?: api.createAccount(request)
            Unit
        }

    override suspend fun getAnalytics(query: AnalyticsQuery): AnalyticsLoadResult =
        try {
            withContext(ioDispatcher) {
                val accounts = api.getAccounts().map { it.toDomain() }
                val requestedAccounts =
                    query.accountId?.let { selectedId -> accounts.filter { it.id == selectedId } } ?: accounts
                val transactions =
                    coroutineScope {
                        requestedAccounts
                            .map { account ->
                                async {
                                    api.getTransactions(
                                        accountId = account.id,
                                        startDate = query.startDate.toString(),
                                        endDate = query.endDate.toString(),
                                    )
                                }
                            }.awaitAll()
                            .flatten()
                            .map { it.toDomain() }
                    }
                val kindFiltered =
                    transactions.filter { transaction ->
                        when (query.transactionKind) {
                            AnalyticsTransactionKind.Income -> transaction.category.isIncome
                            AnalyticsTransactionKind.Expense -> !transaction.category.isIncome
                            AnalyticsTransactionKind.All -> true
                        }
                    }
                val availableCategories =
                    kindFiltered
                        .map { it.category }
                        .distinctBy { it.id }
                        .sortedBy { it.name }
                val filtered =
                    kindFiltered
                        .filter { transaction ->
                            query.categoryIds.isEmpty() || transaction.category.id in query.categoryIds
                        }.sortedByDescending { it.transactionDate }

                AnalyticsLoadResult.Success(
                    summary =
                        AnalyticsSummary(
                            total = filtered.fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount },
                            currencyCode = query.currencyCode,
                            transactions = filtered,
                            accounts = accounts,
                            availableCategories = availableCategories,
                        ),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            AnalyticsLoadResult.Failure(FinanceFailureReason.Network)
        } catch (error: HttpException) {
            AnalyticsLoadResult.Failure(error.toFailureReason())
        } catch (_: Exception) {
            AnalyticsLoadResult.Failure(FinanceFailureReason.Unknown)
        }

    override suspend fun getDailySummary(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult =
        try {
            withContext(ioDispatcher) {
                val accounts = api.getAccounts()
                val requestDate = date.toString()
                val finance =
                    coroutineScope {
                        accounts
                            .map { account ->
                                async {
                                    api.getTransactions(
                                        accountId = account.id,
                                        startDate = requestDate,
                                        endDate = requestDate,
                                    )
                                }
                            }.awaitAll()
                            .flatten()
                            .map { it.toDomain() }
                            .filter { transaction ->
                                when (transactionKind) {
                                    TransactionKind.Income -> transaction.category.isIncome
                                    TransactionKind.Expense -> !transaction.category.isIncome
                                }
                            }.sortedByDescending { it.transactionDate }
                    }

                FinanceLoadResult.Success(
                    summary =
                        FinanceSummary(
                            total =
                                finance.fold(BigDecimal.ZERO) { total, transaction ->
                                    total + transaction.amount
                                },
                            currencyCode = currencyCode,
                            transactions = finance,
                        ),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            FinanceLoadResult.Failure(FinanceFailureReason.Network)
        } catch (error: HttpException) {
            FinanceLoadResult.Failure(error.toFailureReason())
        } catch (_: Exception) {
            FinanceLoadResult.Failure(FinanceFailureReason.Unknown)
        }

    override suspend fun getAccountsSummary(currencyCode: String): AccountsLoadResult =
        try {
            withContext(ioDispatcher) {
                val accounts = api.getAccounts().map { it.toDomain() }
                AccountsLoadResult.Success(
                    summary =
                        AccountsSummary(
                            total =
                                accounts.fold(BigDecimal.ZERO) { total, account ->
                                    total + account.balance
                                },
                            currencyCode = currencyCode,
                            accounts = accounts,
                        ),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            AccountsLoadResult.Failure(FinanceFailureReason.Network)
        } catch (error: HttpException) {
            AccountsLoadResult.Failure(error.toFailureReason())
        } catch (_: Exception) {
            AccountsLoadResult.Failure(FinanceFailureReason.Unknown)
        }
}

private suspend inline fun <T> editorRequest(
    dispatcher: CoroutineDispatcher,
    crossinline block: suspend () -> T,
): EditorResult<T> =
    try {
        EditorResult.Success(withContext(dispatcher) { block() })
    } catch (error: CancellationException) {
        throw error
    } catch (_: IOException) {
        EditorResult.Failure(FinanceFailureReason.Network)
    } catch (error: HttpException) {
        EditorResult.Failure(error.toFailureReason())
    } catch (_: Exception) {
        EditorResult.Failure(FinanceFailureReason.Unknown)
    }

private fun HttpException.toFailureReason(): FinanceFailureReason =
    when (code()) {
        HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> FinanceFailureReason.Authorization
        in HttpStatus.SERVER_ERROR_MIN..HttpStatus.SERVER_ERROR_MAX -> FinanceFailureReason.Server
        else -> FinanceFailureReason.Unknown
    }

private object HttpStatus {
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val SERVER_ERROR_MIN = 500
    const val SERVER_ERROR_MAX = 599
}
