package com.yandex.school.casheye.feature.income.di

import androidx.lifecycle.ViewModel
import com.yandex.school.casheye.feature.income.presentation.AddIncomeViewModel
import com.yandex.school.casheye.feature.income.presentation.IncomeViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
interface IncomeViewModelBindings {
    @Binds
    @IntoMap
    @ViewModelKey(IncomeViewModel::class)
    val IncomeViewModel.bindIncomeViewModel: ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddIncomeViewModel::class)
    val AddIncomeViewModel.bindAddIncomeViewModel: ViewModel
}
