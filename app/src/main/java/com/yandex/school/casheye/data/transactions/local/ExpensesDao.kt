package com.yandex.school.casheye.data.transactions.local

import com.yandex.school.casheye.domain.transactions.model.Expenses
import kotlinx.coroutines.flow.Flow


interface ExpensesDao {

    fun observeExpenses(): Flow<Expenses>

}