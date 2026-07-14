package com.yandex.school.casheye.data.analytics.di

import com.yandex.school.casheye.data.analytics.local.AnalyticsDao
import com.yandex.school.casheye.data.analytics.local.AnalyticsDaoMock
import com.yandex.school.casheye.data.analytics.repository.AnalyticsRepositoryImpl
import com.yandex.school.casheye.domain.analytics.repository.AnalyticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsDataModule {


    @Binds
    @Singleton
    abstract fun bindsAnalyticsDao(analyticsDaoMock: AnalyticsDaoMock): AnalyticsDao


    @Binds
    @Singleton
    abstract fun bindsAnalyticsRepository(analyticsRepositoryImpl: AnalyticsRepositoryImpl): AnalyticsRepository

}
