package com.yandex.school.casheye.data.expenses.repository

import com.yandex.school.casheye.data.expenses.api.ExpensesApi
import com.yandex.school.casheye.data.expenses.mapper.toDomain
import com.yandex.school.casheye.domain.expenses.ExpensesFailureReason
import com.yandex.school.casheye.domain.expenses.ExpensesLoadResult
import com.yandex.school.casheye.domain.expenses.ExpensesRepository
import com.yandex.school.casheye.domain.expenses.ExpensesSummary
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
class ExpensesRepositoryImpl(
    private val api: ExpensesApi,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ExpensesRepository {
    override suspend fun getExpenses(
        date: LocalDate,
        currencyCode: String,
    ): ExpensesLoadResult =
        try {
            withContext(computationDispatcher) {
                val accounts = api.getAccounts()
                val requestDate = date.toString()
                val expenses =
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
                            .filterNot { it.category.isIncome }
                            .sortedByDescending { it.transactionDate }
                    }

                ExpensesLoadResult.Success(
                    summary =
                        ExpensesSummary(
                            total =
                                expenses.fold(BigDecimal.ZERO) { total, transaction ->
                                    total + transaction.amount
                                },
                            currencyCode = currencyCode,
                            transactions = expenses,
                        ),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            ExpensesLoadResult.Failure(ExpensesFailureReason.Network)
        } catch (error: HttpException) {
            ExpensesLoadResult.Failure(error.toFailureReason())
        } catch (_: Exception) {
            ExpensesLoadResult.Failure(ExpensesFailureReason.Unknown)
        }
}

private fun HttpException.toFailureReason(): ExpensesFailureReason =
    when (code()) {
        401, 403 -> ExpensesFailureReason.Authorization
        in 500..599 -> ExpensesFailureReason.Server
        else -> ExpensesFailureReason.Unknown
    }
