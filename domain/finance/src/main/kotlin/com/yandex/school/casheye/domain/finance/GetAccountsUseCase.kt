package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class GetAccountsUseCase(
    private val repository: FinanceRepository,
) {
    operator fun invoke(currencyCode: String): Flow<AccountsLoadResult> =
        repository
            .observeAccounts()
            .map<List<Account>, AccountsLoadResult> { accounts ->
                AccountsLoadResult.Success(
                    AccountsSummary(
                        total = accounts.fold(BigDecimal.ZERO) { total, account -> total + account.balance },
                        currencyCode = currencyCode,
                        accounts = accounts,
                    ),
                )
            }.catch { emit(AccountsLoadResult.Failure(FinanceFailureReason.Unknown)) }

    suspend fun refresh(): FinanceRefreshResult = repository.refreshAccounts()
}
