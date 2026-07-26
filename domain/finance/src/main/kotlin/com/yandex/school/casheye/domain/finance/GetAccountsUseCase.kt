package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

class GetAccountsUseCase(
    private val repository: FinanceRepository,
    private val reportingCurrencyRepository: ReportingCurrencyRepository = DefaultReportingCurrencyRepository,
) {
    operator fun invoke(): Flow<AccountsLoadResult> =
        combine(
            repository.observeAccounts(),
            reportingCurrencyRepository.observe(),
        ) { accounts, reportingCurrency ->
            val result: AccountsLoadResult =
                AccountsLoadResult.Success(
                    AccountsSummary(
                        total = accounts.fold(BigDecimal.ZERO) { total, account -> total + account.balance },
                        currencyCode = reportingCurrency,
                        accounts = accounts,
                    ),
                )
            result
        }.catch { emit(AccountsLoadResult.Failure(FinanceFailureReason.Unknown)) }

    suspend fun refresh(): FinanceRefreshResult = repository.refreshAccounts()
}
