package com.yandex.school.casheye.domain.transactions.repository

import com.yandex.school.casheye.domain.transactions.model.Income
import kotlinx.coroutines.flow.Flow


interface IncomeRepository {


    fun observeIncome(): Flow<Income>
}