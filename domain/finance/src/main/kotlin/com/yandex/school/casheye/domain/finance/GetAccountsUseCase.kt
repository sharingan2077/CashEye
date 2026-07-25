package com.yandex.school.casheye.domain.finance

import java.math.BigDecimal

class GetAccountsUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(currencyCode: String): AccountsLoadResult {
        val accounts =
            when (val result = repository.getAccounts()) {
                is FinanceDataLoadResult.Success -> result.data
                is FinanceDataLoadResult.Failure -> return AccountsLoadResult.Failure(result.reason)
            }

        return AccountsLoadResult.Success(
            AccountsSummary(
                total = accounts.fold(BigDecimal.ZERO) { total, account -> total + account.balance },
                currencyCode = currencyCode,
                accounts = accounts,
            ),
        )
    }
}
