package com.yandex.school.casheye.data.finance.di

import com.yandex.school.casheye.data.finance.database.FinanceLocalStore
import com.yandex.school.casheye.data.finance.database.RoomFinanceLocalStore
import com.yandex.school.casheye.data.finance.repository.FinanceRepositoryImpl
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAccountUseCase
import com.yandex.school.casheye.domain.finance.GetAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetAnalyticsUseCase
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.GetEditorAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetEditorCategoriesUseCase
import com.yandex.school.casheye.domain.finance.GetTransactionUseCase
import com.yandex.school.casheye.domain.finance.SaveAccountUseCase
import com.yandex.school.casheye.domain.finance.SaveTransactionUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides

@BindingContainer
interface FinanceRepositoryBindings {
    @Binds
    val FinanceRepositoryImpl.bind: FinanceRepository

    @Binds
    val RoomFinanceLocalStore.bindLocalStore: FinanceLocalStore
}

@BindingContainer
object FinanceUseCaseBindings {
    @Provides
    fun provideGetDailySummaryUseCase(repository: FinanceRepository): GetDailySummaryUseCase =
        GetDailySummaryUseCase(repository)

    @Provides
    fun provideGetAccountsUseCase(repository: FinanceRepository): GetAccountsUseCase = GetAccountsUseCase(repository)

    @Provides
    fun provideGetAnalyticsUseCase(repository: FinanceRepository): GetAnalyticsUseCase = GetAnalyticsUseCase(repository)

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
    fun provideGetAccountUseCase(repository: FinanceRepository): GetAccountUseCase = GetAccountUseCase(repository)

    @Provides
    fun provideSaveAccountUseCase(repository: FinanceRepository): SaveAccountUseCase = SaveAccountUseCase(repository)
}
