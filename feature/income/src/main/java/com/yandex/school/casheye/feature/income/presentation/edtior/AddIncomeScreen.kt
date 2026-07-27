package com.yandex.school.casheye.feature.income.presentation.edtior

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.school.casheye.core.designsystem.component.editor.EditorOption
import com.yandex.school.casheye.core.designsystem.component.editor.TransactionEditorSheet
import com.yandex.school.casheye.core.format.formatAmount

@Composable
fun AddIncomeScreen(
    state: AddIncomeUiState,
    onIntent: (AddIncomeIntent) -> Unit,
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
