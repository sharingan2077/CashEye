package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.mapper.toDomain
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.FinanceSummary
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
}

private fun HttpException.toFailureReason(): FinanceFailureReason =
    when (code()) {
        401, 403 -> FinanceFailureReason.Authorization
        in 500..599 -> FinanceFailureReason.Server
        else -> FinanceFailureReason.Unknown
    }
