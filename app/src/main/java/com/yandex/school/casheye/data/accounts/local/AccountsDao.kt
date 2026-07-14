package com.yandex.school.casheye.data.accounts.local

import com.yandex.school.casheye.domain.accounts.model.Accounts
import kotlinx.coroutines.flow.Flow

interface AccountsDao {

    fun observeAccounts(): Flow<Accounts>

}