package com.yandex.school.casheye.feature.income.domain.repository

import com.yandex.school.casheye.feature.income.domain.model.Income
import kotlinx.coroutines.flow.Flow


interface IncomeRepository {


    fun observeIncome(): Flow<Income>
}