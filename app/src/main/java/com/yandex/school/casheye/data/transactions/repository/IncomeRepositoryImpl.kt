package com.yandex.school.casheye.data.transactions.repository

import com.yandex.school.casheye.data.transactions.local.IncomeDao
import com.yandex.school.casheye.domain.transactions.model.Income
import com.yandex.school.casheye.domain.transactions.repository.IncomeRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class IncomeRepositoryImpl @Inject constructor(private val dao: IncomeDao) :
    IncomeRepository {

    override fun observeIncome(): Flow<Income> {
        return dao.observeIncome()
    }
}