package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.MoneyAmount
import java.time.LocalDate

data class AccountsSummary(
    val nativeTotals: List<MoneyAmount>,
    val currentValuation: AccountsCurrentValuation?,
    val accounts: List<Account>,
)

data class AccountsCurrentValuation(
    val includedTotal: MoneyAmount?,
    val excludedNativeTotals: List<MoneyAmount>,
    val rateDate: LocalDate?,
) {
    val isComplete: Boolean
        get() = excludedNativeTotals.isEmpty()
}

sealed interface AccountsLoadResult {
    data class Success(
        val summary: AccountsSummary,
    ) : AccountsLoadResult

    data class Failure(
        val reason: FinanceFailureReason,
    ) : AccountsLoadResult
}
