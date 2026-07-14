package com.yandex.school.casheye.data.accounts.di

import com.yandex.school.casheye.data.accounts.local.AccountsDao
import com.yandex.school.casheye.data.accounts.local.AccountsDaoMock
import com.yandex.school.casheye.data.accounts.repository.AccountsRepositoryImpl
import com.yandex.school.casheye.domain.accounts.repository.AccountsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class AccountsDataModule {


    @Binds
    @Singleton
    abstract fun bindsAccountsRepository(
        accountsRepositoryImpl: AccountsRepositoryImpl
    ): AccountsRepository

    @Binds
    @Singleton
    abstract fun bindsAccountsDao(
        accountsDaoMock: AccountsDaoMock
    ): AccountsDao

}
