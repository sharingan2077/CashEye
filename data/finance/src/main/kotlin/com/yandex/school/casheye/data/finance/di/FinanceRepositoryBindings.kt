package com.yandex.school.casheye.data.finance.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.yandex.school.casheye.data.finance.api.ExchangeRateApi
import com.yandex.school.casheye.data.finance.api.FinanceApi
import com.yandex.school.casheye.data.finance.database.FinanceDatabaseProvider
import com.yandex.school.casheye.data.finance.database.FinanceLocalStore
import com.yandex.school.casheye.data.finance.database.RoomFinanceLocalStore
import com.yandex.school.casheye.data.finance.database.RoomFinanceSyncStore
import com.yandex.school.casheye.data.finance.repository.FinanceRepositoryImpl
import com.yandex.school.casheye.data.finance.repository.PreferencesReportingCurrencyRepository
import com.yandex.school.casheye.data.finance.repository.RoomExchangeRateRepository
import com.yandex.school.casheye.data.finance.sync.ExchangeRateRefreshScheduler
import com.yandex.school.casheye.data.finance.sync.ExchangeRateRefresher
import com.yandex.school.casheye.data.finance.sync.FinanceSyncScheduler
import com.yandex.school.casheye.data.finance.sync.FinanceSyncer
import com.yandex.school.casheye.data.finance.sync.WorkManagerExchangeRateRefreshScheduler
import com.yandex.school.casheye.data.finance.sync.WorkManagerFinanceSyncScheduler
import com.yandex.school.casheye.domain.finance.DeleteAccountUseCase
import com.yandex.school.casheye.domain.finance.DeleteTransactionUseCase
import com.yandex.school.casheye.domain.finance.FinanceEditorRepository
import com.yandex.school.casheye.domain.finance.FinanceQueryRepository
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAccountTransactionCountUseCase
import com.yandex.school.casheye.domain.finance.GetAccountUseCase
import com.yandex.school.casheye.domain.finance.GetAccountCurrencyChangeEligibilityUseCase
import com.yandex.school.casheye.domain.finance.GetAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetAnalyticsUseCase
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.GetEditorAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetEditorCategoriesUseCase
import com.yandex.school.casheye.domain.finance.GetTransactionUseCase
import com.yandex.school.casheye.domain.finance.SaveAccountUseCase
import com.yandex.school.casheye.domain.finance.SaveTransactionUseCase
import com.yandex.school.casheye.domain.finance.currency.CurrencyConverter
import com.yandex.school.casheye.domain.finance.currency.ExchangeRateRepository
import com.yandex.school.casheye.domain.finance.currency.ObserveReportingCurrencyUseCase
import com.yandex.school.casheye.domain.finance.currency.ReportingCurrencyRepository
import com.yandex.school.casheye.domain.finance.currency.SetReportingCurrencyUseCase
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
    fun provideExchangeRateRefreshScheduler(
        scheduler: WorkManagerExchangeRateRefreshScheduler,
    ): ExchangeRateRefreshScheduler = scheduler

    @Provides
    @SingleIn(AppScope::class)
    fun provideExchangeRateRepository(
        api: ExchangeRateApi,
        databaseProvider: FinanceDatabaseProvider,
    ): ExchangeRateRepository = RoomExchangeRateRepository(api, databaseProvider)

    @Provides
    @SingleIn(AppScope::class)
    fun provideExchangeRateRefresher(repository: ExchangeRateRepository): ExchangeRateRefresher =
        ExchangeRateRefresher(repository)

    @Provides
    @SingleIn(AppScope::class)
    fun provideFinanceRepository(
        api: FinanceApi,
        localStore: FinanceLocalStore,
        syncScheduler: FinanceSyncScheduler,
    ): FinanceRepository = FinanceRepositoryImpl(api, localStore, Dispatchers.IO, syncScheduler)

    @Provides
    fun provideFinanceQueryRepository(repository: FinanceRepository): FinanceQueryRepository = repository

    @Provides
    fun provideFinanceEditorRepository(repository: FinanceRepository): FinanceEditorRepository = repository
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
    fun provideCurrencyConverter(): CurrencyConverter = CurrencyConverter()

    @Provides
    fun provideGetDailySummaryUseCase(
        repository: FinanceQueryRepository,
        reportingCurrencyRepository: ReportingCurrencyRepository,
    ): GetDailySummaryUseCase = GetDailySummaryUseCase(repository, reportingCurrencyRepository)

    @Provides
    fun provideGetAccountsUseCase(
        repository: FinanceQueryRepository,
        reportingCurrencyRepository: ReportingCurrencyRepository,
        exchangeRateRepository: ExchangeRateRepository,
        currencyConverter: CurrencyConverter,
    ): GetAccountsUseCase =
        GetAccountsUseCase(
            repository = repository,
            reportingCurrencyRepository = reportingCurrencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            currencyConverter = currencyConverter,
        )

    @Provides
    fun provideGetAnalyticsUseCase(
        repository: FinanceQueryRepository,
        reportingCurrencyRepository: ReportingCurrencyRepository,
        exchangeRateRepository: ExchangeRateRepository,
        currencyConverter: CurrencyConverter,
    ): GetAnalyticsUseCase =
        GetAnalyticsUseCase(
            repository = repository,
            reportingCurrencyRepository = reportingCurrencyRepository,
            exchangeRateRepository = exchangeRateRepository,
            currencyConverter = currencyConverter,
        )

    @Provides
    fun provideObserveReportingCurrencyUseCase(
        repository: ReportingCurrencyRepository,
    ): ObserveReportingCurrencyUseCase = ObserveReportingCurrencyUseCase(repository)

    @Provides
    fun provideSetReportingCurrencyUseCase(repository: ReportingCurrencyRepository): SetReportingCurrencyUseCase =
        SetReportingCurrencyUseCase(repository)

    @Provides
    fun provideGetEditorAccountsUseCase(repository: FinanceQueryRepository): GetEditorAccountsUseCase =
        GetEditorAccountsUseCase(repository)

    @Provides
    fun provideGetEditorCategoriesUseCase(repository: FinanceEditorRepository): GetEditorCategoriesUseCase =
        GetEditorCategoriesUseCase(repository)

    @Provides
    fun provideGetTransactionUseCase(repository: FinanceEditorRepository): GetTransactionUseCase =
        GetTransactionUseCase(repository)

    @Provides
    fun provideSaveTransactionUseCase(repository: FinanceEditorRepository): SaveTransactionUseCase =
        SaveTransactionUseCase(repository)

    @Provides
    fun provideDeleteTransactionUseCase(repository: FinanceEditorRepository): DeleteTransactionUseCase =
        DeleteTransactionUseCase(repository)

    @Provides
    fun provideGetAccountUseCase(repository: FinanceEditorRepository): GetAccountUseCase = GetAccountUseCase(repository)

    @Provides
    fun provideSaveAccountUseCase(repository: FinanceEditorRepository): SaveAccountUseCase =
        SaveAccountUseCase(repository)

    @Provides
    fun provideGetAccountCurrencyChangeEligibilityUseCase(
        repository: FinanceEditorRepository,
    ): GetAccountCurrencyChangeEligibilityUseCase =
        GetAccountCurrencyChangeEligibilityUseCase(repository)

    @Provides
    fun provideGetAccountTransactionCountUseCase(
        repository: FinanceEditorRepository,
    ): GetAccountTransactionCountUseCase =
        GetAccountTransactionCountUseCase(repository)

    @Provides
    fun provideDeleteAccountUseCase(repository: FinanceEditorRepository): DeleteAccountUseCase =
        DeleteAccountUseCase(repository)
}
