package com.yandex.school.casheye.feature.income.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.school.casheye.core.designsystem.component.editor.EditorOption
import com.yandex.school.casheye.core.designsystem.component.editor.TransactionEditorSheet

@Composable
fun AddIncomeScreen(
    state: AddIncomeUiState,
    onIntent: (AddIncomeIntent) -> Unit,
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
        onAmountChange = { onIntent(AddIncomeIntent.AmountChanged(it)) },
        onCategoryChange = { onIntent(AddIncomeIntent.CategorySelected(it.id)) },
        onAccountChange = { onIntent(AddIncomeIntent.AccountSelected(it.id)) },
        onDateChange = { onIntent(AddIncomeIntent.DateChanged(it)) },
        onTimeChange = { onIntent(AddIncomeIntent.TimeChanged(it)) },
        onCommentChange = { onIntent(AddIncomeIntent.CommentChanged(it)) },
        onSave = { onIntent(AddIncomeIntent.Save) },
        onDismiss = onDismiss,
    )
}
