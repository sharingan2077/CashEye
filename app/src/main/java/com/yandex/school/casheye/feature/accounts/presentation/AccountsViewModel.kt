package com.yandex.school.casheye.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.domain.accounts.repository.AccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import javax.inject.Inject


data class AccountsUiState(
    val total: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String = "RUB",
    val accounts: List<AccountListItemUi> = emptyList(),
)

data class AccountListItemUi(
    val account: Account,
    val emoji: String,
)


@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountsRepository
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = repository.observeAccounts()
        .map { accounts ->
            AccountsUiState(
                total = accounts.total,
                currencyCode = accounts.currencyCode,
                accounts = accounts.accounts.map { account ->
                    AccountListItemUi(
                        account = account,
                        emoji = "\uD83D\uDCB3"
                    )
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = AccountsUiState()
        )


}