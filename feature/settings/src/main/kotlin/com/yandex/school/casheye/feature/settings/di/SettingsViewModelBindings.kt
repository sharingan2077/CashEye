package com.yandex.school.casheye.feature.settings.di

import androidx.lifecycle.ViewModel
import com.yandex.school.casheye.feature.settings.presentation.AppLockViewModel
import com.yandex.school.casheye.feature.settings.presentation.SettingsViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
interface SettingsViewModelBindings {
    @Binds
    @IntoMap
    @ViewModelKey(AppLockViewModel::class)
    val AppLockViewModel.bindAppLockViewModel: ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    val SettingsViewModel.bindSettingsViewModel: ViewModel
}
