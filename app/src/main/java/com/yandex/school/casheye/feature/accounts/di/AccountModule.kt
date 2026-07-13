package com.yandex.school.casheye.feature.accounts.di

import com.yandex.school.casheye.feature.accounts.data.local.AccountsDao
import com.yandex.school.casheye.feature.accounts.data.local.AccountsDaoMock
import com.yandex.school.casheye.feature.accounts.data.repository.AccountsRepositoryImpl
import com.yandex.school.casheye.feature.accounts.domain.repository.AccountsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {


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