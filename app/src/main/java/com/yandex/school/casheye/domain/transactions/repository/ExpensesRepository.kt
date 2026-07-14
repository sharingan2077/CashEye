package com.yandex.school.casheye.domain.transactions.repository

import com.yandex.school.casheye.domain.transactions.model.Expenses
import kotlinx.coroutines.flow.Flow

interface ExpensesRepository {


    fun observeExpenses(): Flow<Expenses>


}