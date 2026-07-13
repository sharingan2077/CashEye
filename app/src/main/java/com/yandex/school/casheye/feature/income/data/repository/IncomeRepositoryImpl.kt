package com.yandex.school.casheye.feature.income.data.repository

import com.yandex.school.casheye.feature.income.data.local.IncomeDao
import com.yandex.school.casheye.feature.income.domain.model.Income
import com.yandex.school.casheye.feature.income.domain.repository.IncomeRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class IncomeRepositoryImpl @Inject constructor(private val dao: IncomeDao) :
    IncomeRepository {

    override fun observeIncome(): Flow<Income> {
        return dao.observeIncome()
    }
}