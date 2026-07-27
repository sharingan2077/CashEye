package com.yandex.school.casheye.feature.accounts.presentation.efitor

import androidx.annotation.StringRes
import com.yandex.school.casheye.core.model.CurrencyCode

data class AccountEditorUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isCheckingCurrency: Boolean = false,
    val editingId: Int? = null,
    val name: String = "",
    val balance: String = "",
    val currency: CurrencyCode = CurrencyCode.RUB,
    val emoji: String = "💵",
    @StringRes val error: Int? = null,
)

sealed interface AccountEditorIntent {
    data class Open(
        val accountId: Int?,
    ) : AccountEditorIntent

    data class NameChanged(
        val value: String,
    ) : AccountEditorIntent

    data class BalanceChanged(
        val value: String,
    ) : AccountEditorIntent

    data class CurrencyChanged(
        val value: CurrencyCode,
    ) : AccountEditorIntent

    data object CurrencyChangeRequested : AccountEditorIntent

    data class EmojiChanged(
        val value: String,
    ) : AccountEditorIntent

    data object Save : AccountEditorIntent
}

sealed interface AccountEditorEffect {
    data object Saved : AccountEditorEffect

    data object OpenCurrencySelector : AccountEditorEffect
}
