package com.yandex.school.casheye.data.settings.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.yandex.school.casheye.data.settings.repository.PreferencesSettingsRepository
import com.yandex.school.casheye.domain.settings.ObserveSettingsUseCase
import com.yandex.school.casheye.domain.settings.SetBiometricsEnabledUseCase
import com.yandex.school.casheye.domain.settings.SetLanguageUseCase
import com.yandex.school.casheye.domain.settings.SetPinVerifierUseCase
import com.yandex.school.casheye.domain.settings.SetPinUseCase
import com.yandex.school.casheye.domain.settings.SetThemeModeUseCase
import com.yandex.school.casheye.domain.settings.SettingsRepository
import com.yandex.school.casheye.domain.settings.VerifyPinUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
object SettingsRepositoryBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsRepository(context: Context): SettingsRepository =
        PreferencesSettingsRepository(
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("app_settings.preferences_pb") },
            ),
        )

    @Provides
    fun provideObserveSettingsUseCase(repository: SettingsRepository): ObserveSettingsUseCase =
        ObserveSettingsUseCase(repository)

    @Provides
    fun provideSetThemeModeUseCase(repository: SettingsRepository): SetThemeModeUseCase =
        SetThemeModeUseCase(repository)

    @Provides
    fun provideSetLanguageUseCase(repository: SettingsRepository): SetLanguageUseCase =
        SetLanguageUseCase(repository)

    @Provides
    fun provideSetPinVerifierUseCase(repository: SettingsRepository): SetPinVerifierUseCase =
        SetPinVerifierUseCase(repository)

    @Provides
    fun provideSetPinUseCase(repository: SettingsRepository): SetPinUseCase = SetPinUseCase(repository)

    @Provides
    fun provideVerifyPinUseCase(repository: SettingsRepository): VerifyPinUseCase = VerifyPinUseCase(repository)

    @Provides
    fun provideSetBiometricsEnabledUseCase(repository: SettingsRepository): SetBiometricsEnabledUseCase =
        SetBiometricsEnabledUseCase(repository)
}
