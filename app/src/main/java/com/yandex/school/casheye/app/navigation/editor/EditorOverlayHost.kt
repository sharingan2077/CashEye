package com.yandex.school.casheye.app.navigation.editor

import androidx.compose.runtime.Composable
import com.yandex.school.casheye.feature.accounts.presentation.efitor.AccountEditorRoute
import com.yandex.school.casheye.feature.expenses.presentation.editor.AddExpenseRoute
import com.yandex.school.casheye.feature.income.presentation.edtior.AddIncomeRoute
import java.time.LocalDate

@Composable
internal fun EditorOverlayHost(
    target: EditorTarget?,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
) {
    when (target) {
        is EditorTarget.Expense -> {
            AddExpenseRoute(
                transactionId = target.id,
                defaultDate = selectedDate,
                onDismiss = onDismiss,
                onSave = onDismiss,
            )
        }

        is EditorTarget.Income -> {
            AddIncomeRoute(
                transactionId = target.id,
                defaultDate = selectedDate,
                onDismiss = onDismiss,
                onSave = onDismiss,
            )
        }

        is EditorTarget.Account -> {
            AccountEditorRoute(
                accountId = target.id,
                onDismiss = onDismiss,
                onSave = onDismiss,
            )
        }

        null -> {
        }
    }
}

internal sealed interface EditorTarget {
    data class Expense(
        val id: Int?,
    ) : EditorTarget

    data class Income(
        val id: Int?,
    ) : EditorTarget

    data class Account(
        val id: Int?,
    ) : EditorTarget
}
