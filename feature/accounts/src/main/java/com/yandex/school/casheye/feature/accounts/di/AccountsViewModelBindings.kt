package com.yandex.school.casheye.feature.accounts.di

import androidx.lifecycle.ViewModel
import com.yandex.school.casheye.feature.accounts.presentation.AccountsViewModel
import com.yandex.school.casheye.feature.accounts.presentation.efitor.AccountEditorViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
interface AccountsViewModelBindings {
    @Binds
    @IntoMap
    @ViewModelKey(AccountsViewModel::class)
    val AccountsViewModel.bindAccountsViewModel: ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AccountEditorViewModel::class)
    val AccountEditorViewModel.bindAccountEditorViewModel: ViewModel
}
