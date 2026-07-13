package com.yandex.school.casheye.feature.expenses.di

import com.yandex.school.casheye.feature.expenses.data.local.ExpensesDao
import com.yandex.school.casheye.feature.expenses.data.local.ExpensesDaoMock
import com.yandex.school.casheye.feature.expenses.data.repository.ExpensesRepositoryImpl
import com.yandex.school.casheye.feature.expenses.domain.repository.ExpensesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class ExpensesModule {


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
}