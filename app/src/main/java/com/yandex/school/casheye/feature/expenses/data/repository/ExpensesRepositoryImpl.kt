package com.yandex.school.casheye.feature.expenses.data.repository

import com.yandex.school.casheye.feature.expenses.data.local.ExpensesDao
import com.yandex.school.casheye.feature.expenses.domain.model.Expenses
import com.yandex.school.casheye.feature.expenses.domain.repository.ExpensesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExpensesRepositoryImpl @Inject constructor(
    private val dao: ExpensesDao
) : ExpensesRepository {

    override fun observeExpenses(): Flow<Expenses> {
        return dao.observeExpenses()
    }


}