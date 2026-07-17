package com.yandex.school.casheye.data.expenses.di

import com.yandex.school.casheye.data.expenses.repository.ExpensesRepositoryImpl
import com.yandex.school.casheye.domain.expenses.ExpensesRepository
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds

@BindingContainer
interface ExpensesRepositoryBindings {
    @Binds
    val ExpensesRepositoryImpl.bind: ExpensesRepository
}
