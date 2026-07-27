package com.yandex.school.casheye.feature.accounts.presentation.efitor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.school.casheye.core.model.CurrencyCode

@Composable
fun AccountEditorScreen(
    state: AccountEditorUiState,
    onIntent: (AccountEditorIntent) -> Unit,
    currencySelectionRequest: Int,
    onDismiss: () -> Unit,
) {
    AccountEditorSheet(
        name = state.name,
        balance = state.balance,
        currency = state.currency.isoCode,
        emoji = state.emoji,
        isEditing = state.editingId != null,
        isSaving = state.isSaving || state.isLoading || state.isCheckingCurrency,
        error = state.error?.let { stringResource(it) },
        onNameChange = { onIntent(AccountEditorIntent.NameChanged(it)) },
        onBalanceChange = { onIntent(AccountEditorIntent.BalanceChanged(it)) },
        onCurrencyChange = {
            onIntent(AccountEditorIntent.CurrencyChanged(CurrencyCode.fromIsoCode(it)))
        },
        currencySelectionRequest = currencySelectionRequest,
        onCurrencyRequested = { onIntent(AccountEditorIntent.CurrencyChangeRequested) },
        onEmojiChange = { onIntent(AccountEditorIntent.EmojiChanged(it)) },
        onSave = { onIntent(AccountEditorIntent.Save) },
        onDismiss = onDismiss,
    )
}
