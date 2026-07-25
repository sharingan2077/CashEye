package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.mapper.toDomain
import com.yandex.school.casheye.domain.finance.FinanceDataLoadResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.TransactionsQuery
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

@Inject
@SingleIn(AppScope::class)
class FinanceRepositoryImpl(
    private val api: FinanceApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FinanceRepository {
    override suspend fun getAccounts(): FinanceDataLoadResult<List<Account>> =
        try {
            withContext(ioDispatcher) {
                FinanceDataLoadResult.Success(api.getAccounts().map { it.toDomain() })
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            FinanceDataLoadResult.Failure(FinanceFailureReason.Network)
        } catch (error: HttpException) {
            FinanceDataLoadResult.Failure(error.toFailureReason())
        } catch (_: Exception) {
            FinanceDataLoadResult.Failure(FinanceFailureReason.Unknown)
        }

    override suspend fun getTransactions(query: TransactionsQuery): FinanceDataLoadResult<List<Transaction>> =
        try {
            withContext(ioDispatcher) {
                val transactions =
                    coroutineScope {
                        query.accountIds
                            .map { accountId ->
                                async {
                                    api.getTransactions(
                                        accountId = accountId,
                                        startDate = query.startDate.toString(),
                                        endDate = query.endDate.toString(),
                                    )
                                }
                            }.awaitAll()
                            .flatten()
                            .map { it.toDomain() }
                    }
                FinanceDataLoadResult.Success(transactions)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            FinanceDataLoadResult.Failure(FinanceFailureReason.Network)
        } catch (error: HttpException) {
            FinanceDataLoadResult.Failure(error.toFailureReason())
        } catch (_: Exception) {
            FinanceDataLoadResult.Failure(FinanceFailureReason.Unknown)
        }
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
