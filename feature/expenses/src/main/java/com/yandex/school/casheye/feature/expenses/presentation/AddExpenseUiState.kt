package com.yandex.school.casheye.feature.expenses.presentation

import androidx.annotation.StringRes
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import java.time.LocalDate
import java.time.LocalTime

data class AddExpenseUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editingId: Int? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: Int? = null,
    val selectedCategoryId: Int? = null,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val comment: String = "",
    @StringRes val error: Int? = null,
)

sealed interface AddExpenseIntent {
    data class Open(
        val transactionId: Int?,
        val defaultDate: LocalDate,
    ) : AddExpenseIntent

    data class AmountChanged(
        val value: String,
    ) : AddExpenseIntent

    data class AccountSelected(
        val id: Int,
    ) : AddExpenseIntent

    data class CategorySelected(
        val id: Int,
    ) : AddExpenseIntent

    data class DateChanged(
        val value: LocalDate,
    ) : AddExpenseIntent

    data class TimeChanged(
        val value: LocalTime,
    ) : AddExpenseIntent

    data class CommentChanged(
        val value: String,
    ) : AddExpenseIntent

    data object Save : AddExpenseIntent
}

sealed interface AddExpenseEffect {
    data object Saved : AddExpenseEffect
}
