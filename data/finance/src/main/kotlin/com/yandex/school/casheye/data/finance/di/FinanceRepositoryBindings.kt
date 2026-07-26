package com.yandex.school.casheye.data.finance.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceLocalStore
import com.yandex.school.casheye.data.finance.database.RoomFinanceLocalStore
import com.yandex.school.casheye.data.finance.database.RoomFinanceSyncStore
import com.yandex.school.casheye.data.finance.repository.FinanceRepositoryImpl
import com.yandex.school.casheye.data.finance.repository.PreferencesReportingCurrencyRepository
import com.yandex.school.casheye.data.finance.sync.FinanceSyncScheduler
import com.yandex.school.casheye.data.finance.sync.FinanceSyncer
import com.yandex.school.casheye.data.finance.sync.WorkManagerFinanceSyncScheduler
import com.yandex.school.casheye.domain.finance.DeleteAccountUseCase
import com.yandex.school.casheye.domain.finance.DeleteTransactionUseCase
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAccountTransactionCountUseCase
import com.yandex.school.casheye.domain.finance.GetAccountUseCase
import com.yandex.school.casheye.domain.finance.GetAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetAnalyticsUseCase
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.GetEditorAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetEditorCategoriesUseCase
import com.yandex.school.casheye.domain.finance.GetTransactionUseCase
import com.yandex.school.casheye.domain.finance.ObserveReportingCurrencyUseCase
import com.yandex.school.casheye.domain.finance.ReportingCurrencyRepository
import com.yandex.school.casheye.domain.finance.SaveAccountUseCase
import com.yandex.school.casheye.domain.finance.SaveTransactionUseCase
import com.yandex.school.casheye.domain.finance.SetReportingCurrencyUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@BindingContainer
object FinanceRepositoryBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideReportingCurrencyRepository(context: Context): ReportingCurrencyRepository =
        PreferencesReportingCurrencyRepository(
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("reporting_currency.preferences_pb") },
            ),
        )

    @Provides
    fun provideLocalStore(localStore: RoomFinanceLocalStore): FinanceLocalStore = localStore

    @Provides
    fun provideSyncScheduler(scheduler: WorkManagerFinanceSyncScheduler): FinanceSyncScheduler = scheduler

    @Provides
    @SingleIn(AppScope::class)
    fun provideFinanceRepository(
        api: FinanceApi,
        localStore: FinanceLocalStore,
        syncScheduler: FinanceSyncScheduler,
    ): FinanceRepository = FinanceRepositoryImpl(api, localStore, Dispatchers.IO, syncScheduler)
}

@BindingContainer
object FinanceSyncBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideFinanceSyncer(
        api: FinanceApi,
        localStore: RoomFinanceLocalStore,
    ): FinanceSyncer = FinanceSyncer(api, RoomFinanceSyncStore(localStore))
}

@BindingContainer
object FinanceUseCaseBindings {
    @Provides
    fun provideGetDailySummaryUseCase(
        repository: FinanceRepository,
        reportingCurrencyRepository: ReportingCurrencyRepository,
    ): GetDailySummaryUseCase = GetDailySummaryUseCase(repository, reportingCurrencyRepository)

    @Provides
    fun provideGetAccountsUseCase(
        repository: FinanceRepository,
        reportingCurrencyRepository: ReportingCurrencyRepository,
    ): GetAccountsUseCase = GetAccountsUseCase(repository, reportingCurrencyRepository)

    @Provides
    fun provideGetAnalyticsUseCase(
        repository: FinanceRepository,
        reportingCurrencyRepository: ReportingCurrencyRepository,
    ): GetAnalyticsUseCase = GetAnalyticsUseCase(repository, reportingCurrencyRepository)

    @Provides
    fun provideObserveReportingCurrencyUseCase(
        repository: ReportingCurrencyRepository,
    ): ObserveReportingCurrencyUseCase = ObserveReportingCurrencyUseCase(repository)

    @Provides
    fun provideSetReportingCurrencyUseCase(repository: ReportingCurrencyRepository): SetReportingCurrencyUseCase =
        SetReportingCurrencyUseCase(repository)

    @Provides
    fun provideGetEditorAccountsUseCase(repository: FinanceRepository): GetEditorAccountsUseCase =
        GetEditorAccountsUseCase(repository)

    @Provides
    fun provideGetEditorCategoriesUseCase(repository: FinanceRepository): GetEditorCategoriesUseCase =
        GetEditorCategoriesUseCase(repository)

    @Provides
    fun provideGetTransactionUseCase(repository: FinanceRepository): GetTransactionUseCase =
        GetTransactionUseCase(repository)

    @Provides
    fun provideSaveTransactionUseCase(repository: FinanceRepository): SaveTransactionUseCase =
        SaveTransactionUseCase(repository)

    @Provides
    fun provideDeleteTransactionUseCase(repository: FinanceRepository): DeleteTransactionUseCase =
        DeleteTransactionUseCase(repository)

    @Provides
    fun provideGetAccountUseCase(repository: FinanceRepository): GetAccountUseCase = GetAccountUseCase(repository)

    @Provides
    fun provideSaveAccountUseCase(repository: FinanceRepository): SaveAccountUseCase = SaveAccountUseCase(repository)

    @Provides
    fun provideGetAccountTransactionCountUseCase(repository: FinanceRepository): GetAccountTransactionCountUseCase =
        GetAccountTransactionCountUseCase(repository)

    @Provides
    fun provideDeleteAccountUseCase(repository: FinanceRepository): DeleteAccountUseCase =
        DeleteAccountUseCase(repository)
}
