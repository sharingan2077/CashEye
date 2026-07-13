package com.yandex.school.casheye.feature.accounts.data.repository

import com.yandex.school.casheye.feature.accounts.data.local.AccountsDao
import com.yandex.school.casheye.feature.accounts.domain.model.Accounts
import com.yandex.school.casheye.feature.accounts.domain.repository.AccountsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccountsRepositoryImpl @Inject constructor(
    private val dao: AccountsDao
) : AccountsRepository {
    override fun observeAccounts(): Flow<Accounts> {
        return dao.observeAccounts()
    }


}