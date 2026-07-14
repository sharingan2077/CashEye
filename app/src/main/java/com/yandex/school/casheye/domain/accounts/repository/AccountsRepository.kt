package com.yandex.school.casheye.domain.accounts.repository

import com.yandex.school.casheye.domain.accounts.model.Accounts
import kotlinx.coroutines.flow.Flow

interface AccountsRepository {

    fun observeAccounts(): Flow<Accounts>

}