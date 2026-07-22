package com.yandex.school.casheye.feature.expenses.di

import androidx.lifecycle.ViewModel
import com.yandex.school.casheye.feature.expenses.presentation.AddExpenseViewModel
import com.yandex.school.casheye.feature.expenses.presentation.ExpensesViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
interface ExpensesViewModelBindings {
    @Binds
    @IntoMap
    @ViewModelKey(ExpensesViewModel::class)
    val ExpensesViewModel.bindExpensesViewModel: ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddExpenseViewModel::class)
    val AddExpenseViewModel.bindAddExpenseViewModel: ViewModel
}
