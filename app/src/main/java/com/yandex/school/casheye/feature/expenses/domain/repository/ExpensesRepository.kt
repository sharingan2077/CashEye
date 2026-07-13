package com.yandex.school.casheye.feature.expenses.domain.repository

import com.yandex.school.casheye.feature.expenses.domain.model.Expenses
import kotlinx.coroutines.flow.Flow

interface ExpensesRepository {


    fun observeExpenses(): Flow<Expenses>


}