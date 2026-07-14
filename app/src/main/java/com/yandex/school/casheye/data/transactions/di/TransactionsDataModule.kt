package com.yandex.school.casheye.data.transactions.di

import com.yandex.school.casheye.data.transactions.local.ExpensesDao
import com.yandex.school.casheye.data.transactions.local.ExpensesDaoMock
import com.yandex.school.casheye.data.transactions.local.IncomeDao
import com.yandex.school.casheye.data.transactions.local.IncomeDaoMock
import com.yandex.school.casheye.data.transactions.repository.ExpensesRepositoryImpl
import com.yandex.school.casheye.data.transactions.repository.IncomeRepositoryImpl
import com.yandex.school.casheye.domain.transactions.repository.ExpensesRepository
import com.yandex.school.casheye.domain.transactions.repository.IncomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionsDataModule {


    @Binds
    @Singleton
    abstract fun bindExpensesRepository(
        expensesRepositoryImpl: ExpensesRepositoryImpl
    ): ExpensesRepository


    @Binds
    @Singleton
    abstract fun bindExpensesDao(
        expensesDaoMock: ExpensesDaoMock
    ): ExpensesDao

    @Binds
    @Singleton
    abstract fun bindIncomeRepository(
        incomeRepositoryImpl: IncomeRepositoryImpl
    ): IncomeRepository

    @Binds
    @Singleton
    abstract fun bindIncomeDao(
        incomeDaoMock: IncomeDaoMock
    ): IncomeDao
}
