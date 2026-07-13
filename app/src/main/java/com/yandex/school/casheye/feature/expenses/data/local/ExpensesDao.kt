package com.yandex.school.casheye.feature.expenses.data.local

import com.yandex.school.casheye.feature.expenses.domain.model.Expenses
import kotlinx.coroutines.flow.Flow


interface ExpensesDao {

    fun observeExpenses(): Flow<Expenses>

}