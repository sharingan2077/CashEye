package com.yandex.school.casheye.feature.expenses.di

import androidx.lifecycle.ViewModel
import com.yandex.school.casheye.domain.expenses.ExpensesRepository
import com.yandex.school.casheye.domain.expenses.GetExpensesUseCase
import com.yandex.school.casheye.feature.expenses.presentation.ExpensesViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
interface ExpensesViewModelBindings {
    @Binds
    @IntoMap
    @ViewModelKey(ExpensesViewModel::class)
    val ExpensesViewModel.bindExpensesViewModel: ViewModel
}

@BindingContainer
object ExpensesUseCaseBindings {
    @Provides
    fun provideGetExpensesUseCase(repository: ExpensesRepository): GetExpensesUseCase =
        GetExpensesUseCase(repository)
}
