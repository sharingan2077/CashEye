package com.yandex.school.casheye.feature.accounts.data.local

import com.yandex.school.casheye.feature.accounts.domain.model.Accounts
import kotlinx.coroutines.flow.Flow

interface AccountsDao {

    fun observeAccounts(): Flow<Accounts>

}