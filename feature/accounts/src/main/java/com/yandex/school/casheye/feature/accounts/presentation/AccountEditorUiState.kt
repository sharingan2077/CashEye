package com.yandex.school.casheye.feature.accounts.presentation

import androidx.annotation.StringRes
import java.time.LocalDate
import java.time.LocalTime

data class AccountEditorUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editingId: Int? = null,
    val name: String = "",
    val balance: String = "",
    val currency: String = "RUB",
    val emoji: String = "💵",
    val openedDate: LocalDate = LocalDate.now(),
    val openedTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
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
        val value: String,
    ) : AccountEditorIntent

    data object Save : AccountEditorIntent
}

sealed interface AccountEditorEffect {
    data object Saved : AccountEditorEffect
}
