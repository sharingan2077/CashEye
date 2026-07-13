package com.yandex.school.casheye.feature.income.data.local

import com.yandex.school.casheye.feature.income.domain.model.Income
import kotlinx.coroutines.flow.Flow


interface IncomeDao {

    fun observeIncome(): Flow<Income>
}