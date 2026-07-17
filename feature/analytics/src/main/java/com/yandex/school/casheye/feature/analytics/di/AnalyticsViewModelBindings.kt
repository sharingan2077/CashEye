package com.yandex.school.casheye.feature.analytics.di

import androidx.lifecycle.ViewModel
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
interface AnalyticsViewModelBindings {
    @Binds
    @IntoMap
    @ViewModelKey(AnalyticsViewModel::class)
    val AnalyticsViewModel.bindAnalyticsViewModel: ViewModel
}
