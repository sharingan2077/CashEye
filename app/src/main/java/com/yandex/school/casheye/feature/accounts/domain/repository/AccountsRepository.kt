package com.yandex.school.casheye.feature.accounts.domain.repository

import com.yandex.school.casheye.feature.accounts.domain.model.Accounts
import kotlinx.coroutines.flow.Flow

interface AccountsRepository {

    fun observeAccounts(): Flow<Accounts>

}