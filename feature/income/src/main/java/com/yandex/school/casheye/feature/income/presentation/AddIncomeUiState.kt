package com.yandex.school.casheye.feature.income.presentation

import androidx.annotation.StringRes
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import java.time.LocalDate
import java.time.LocalTime

data class AddIncomeUiState(
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

sealed interface AddIncomeIntent {
    data class Open(
        val transactionId: Int?,
        val defaultDate: LocalDate,
    ) : AddIncomeIntent

    data class AmountChanged(
        val value: String,
    ) : AddIncomeIntent

    data class AccountSelected(
        val id: Int,
    ) : AddIncomeIntent

    data class CategorySelected(
        val id: Int,
    ) : AddIncomeIntent

    data class DateChanged(
        val value: LocalDate,
    ) : AddIncomeIntent

    data class TimeChanged(
        val value: LocalTime,
    ) : AddIncomeIntent

    data class CommentChanged(
        val value: String,
    ) : AddIncomeIntent

    data object Save : AddIncomeIntent
}

sealed interface AddIncomeEffect {
    data object Saved : AddIncomeEffect
}
