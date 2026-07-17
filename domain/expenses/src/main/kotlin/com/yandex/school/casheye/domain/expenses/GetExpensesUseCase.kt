package com.yandex.school.casheye.domain.expenses

import java.time.LocalDate

class GetExpensesUseCase(
    private val repository: ExpensesRepository,
) {
    suspend operator fun invoke(
        date: LocalDate,
        currencyCode: String,
    ): ExpensesLoadResult = repository.getExpenses(date, currencyCode)
}
