package com.yandex.school.casheye.feature.accounts.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import java.math.BigDecimal

sealed interface AccountsUiState {
    val isRefreshing: Boolean
        get() = false

    data object Loading : AccountsUiState

    data class Empty(
        override val isRefreshing: Boolean = false,
    ) : AccountsUiState

    data class Content(
        val total: BigDecimal,
        val currencyCode: String,
        val accounts: List<Account>,
        override val isRefreshing: Boolean = false,
    ) : AccountsUiState

    data class Error(
        val reason: FinanceFailureReason,
    ) : AccountsUiState
}

sealed interface AccountsIntent {
    data object Retry : AccountsIntent

    data object Refresh : AccountsIntent
}

sealed interface AccountsEffect {
    data class ShowError(
        val reason: FinanceFailureReason,
    ) : AccountsEffect
}
