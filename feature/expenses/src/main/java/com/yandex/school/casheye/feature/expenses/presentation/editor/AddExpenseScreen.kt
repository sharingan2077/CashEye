package com.yandex.school.casheye.feature.expenses.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.school.casheye.core.designsystem.component.editor.EditorOption
import com.yandex.school.casheye.core.designsystem.component.editor.TransactionEditorSheet
import com.yandex.school.casheye.core.format.formatAmount

@Composable
fun AddExpenseScreen(
    state: AddExpenseUiState,
    onIntent: (AddExpenseIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val accountOptions =
        state.accounts.map {
            EditorOption(
                id = it.id,
                label = it.name,
                emoji = it.emoji,
                currencyCode = it.currency.isoCode,
                subtitle = formatAmount(it.balance, it.currency.isoCode),
            )
        }
    TransactionEditorSheet(
        amount = state.amount,
        category =
            state.categories
                .firstOrNull {
                    it.id == state.selectedCategoryId
                }?.let { EditorOption(it.id, it.name, it.emoji) },
        categories = state.categories.map { EditorOption(it.id, it.name, it.emoji) },
        account = accountOptions.firstOrNull { it.id == state.selectedAccountId },
        accounts = accountOptions,
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
