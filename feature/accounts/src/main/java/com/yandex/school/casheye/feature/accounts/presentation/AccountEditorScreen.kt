package com.yandex.school.casheye.feature.accounts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.school.casheye.core.designsystem.component.AccountEditorSheet

@Composable
fun AccountEditorScreen(
    state: AccountEditorUiState,
    onIntent: (AccountEditorIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    AccountEditorSheet(
        name = state.name,
        balance = state.balance,
        currency = state.currency,
        isEditing = state.editingId != null,
        isSaving = state.isSaving || state.isLoading,
        error = state.error?.let { stringResource(it) },
        onNameChange = { onIntent(AccountEditorIntent.NameChanged(it)) },
        onBalanceChange = { onIntent(AccountEditorIntent.BalanceChanged(it)) },
        onCurrencyChange = { onIntent(AccountEditorIntent.CurrencyChanged(it)) },
        onSave = { onIntent(AccountEditorIntent.Save) },
        onDismiss = onDismiss,
    )
}
