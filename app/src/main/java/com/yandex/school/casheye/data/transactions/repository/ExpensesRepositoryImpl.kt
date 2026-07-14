package com.yandex.school.casheye.data.transactions.repository

import com.yandex.school.casheye.data.transactions.local.ExpensesDao
import com.yandex.school.casheye.domain.transactions.model.Expenses
import com.yandex.school.casheye.domain.transactions.repository.ExpensesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExpensesRepositoryImpl @Inject constructor(
    private val dao: ExpensesDao
) : ExpensesRepository {

    override fun observeExpenses(): Flow<Expenses> {
        return dao.observeExpenses()
    }


}