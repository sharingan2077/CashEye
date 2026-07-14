package com.yandex.school.casheye.data.accounts.repository

import com.yandex.school.casheye.data.accounts.local.AccountsDao
import com.yandex.school.casheye.domain.accounts.model.Accounts
import com.yandex.school.casheye.domain.accounts.repository.AccountsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccountsRepositoryImpl @Inject constructor(
    private val dao: AccountsDao
) : AccountsRepository {
    override fun observeAccounts(): Flow<Accounts> {
        return dao.observeAccounts()
    }


}