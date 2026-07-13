package com.yandex.school.casheye.feature.income.di

import com.yandex.school.casheye.feature.income.data.local.IncomeDao
import com.yandex.school.casheye.feature.income.data.local.IncomeDaoMock
import com.yandex.school.casheye.feature.income.data.repository.IncomeRepositoryImpl
import com.yandex.school.casheye.feature.income.domain.repository.IncomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class IncomeModule {


    @Binds
    @Singleton
    abstract fun bindsIncomeDao(incomeDaoMock: IncomeDaoMock): IncomeDao


    @Binds
    @Singleton
    abstract fun bindsIncomeRepository(incomeRepositoryImpl: IncomeRepositoryImpl): IncomeRepository
}