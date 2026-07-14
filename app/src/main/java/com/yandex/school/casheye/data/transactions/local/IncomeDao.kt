package com.yandex.school.casheye.data.transactions.local

import com.yandex.school.casheye.domain.transactions.model.Income
import kotlinx.coroutines.flow.Flow


interface IncomeDao {

    fun observeIncome(): Flow<Income>
}