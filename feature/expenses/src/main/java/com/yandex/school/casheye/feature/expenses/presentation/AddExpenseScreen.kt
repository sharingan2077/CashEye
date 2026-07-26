package com.yandex.school.casheye.feature.expenses.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.school.casheye.core.designsystem.component.EditorOption
import com.yandex.school.casheye.core.designsystem.component.TransactionEditorSheet

@Composable
fun AddExpenseScreen(
    state: AddExpenseUiState,
    onIntent: (AddExpenseIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    TransactionEditorSheet(
        amount = state.amount,
        category =
            state.categories
                .firstOrNull {
                    it.id == state.selectedCategoryId
                }?.let { EditorOption(it.id, it.name, it.emoji) },
        categories = state.categories.map { EditorOption(it.id, it.name, it.emoji) },
        account =
            state.accounts.firstOrNull { it.id == state.selectedAccountId }?.let {
                EditorOption(it.id, it.name, it.emoji, it.currency.isoCode)
            },
        accounts = state.accounts.map { EditorOption(it.id, it.name, it.emoji, it.currency.isoCode) },
        date = state.date,
        time = state.time,
        comment = state.comment,
        isSaving = state.isSaving || state.isLoading,
        error = state.error?.let { stringResource(it) },
        onAmountChange = { onIntent(AddExpenseIntent.AmountChanged(it)) },
        onCategoryChange = { onIntent(AddExpenseIntent.CategorySelected(it.id)) },
        onAccountChange = { onIntent(AddExpenseIntent.AccountSelected(it.id)) },
        onDateChange = { onIntent(AddExpenseIntent.DateChanged(it)) },
        onTimeChange = { onIntent(AddExpenseIntent.TimeChanged(it)) },
        onCommentChange = { onIntent(AddExpenseIntent.CommentChanged(it)) },
        onSave = { onIntent(AddExpenseIntent.Save) },
        onDismiss = onDismiss,
    )
}
