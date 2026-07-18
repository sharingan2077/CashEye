package com.yandex.school.casheye.data.finance.di

import com.yandex.school.casheye.data.finance.repository.FinanceRepositoryImpl
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides

@BindingContainer
interface FinanceRepositoryBindings {
    @Binds
    val FinanceRepositoryImpl.bind: FinanceRepository
}

@BindingContainer
object FinanceUseCaseBindings {
    @Provides
    fun provideGetDailySummaryUseCase(repository: FinanceRepository): GetDailySummaryUseCase =
        GetDailySummaryUseCase(repository)
}
