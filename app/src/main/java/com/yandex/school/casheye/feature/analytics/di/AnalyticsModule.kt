package com.yandex.school.casheye.feature.analytics.di

import com.yandex.school.casheye.feature.analytics.data.local.AnalyticsDao
import com.yandex.school.casheye.feature.analytics.data.local.AnalyticsDaoMock
import com.yandex.school.casheye.feature.analytics.data.repository.AnalyticsRepositoryImpl
import com.yandex.school.casheye.feature.analytics.domain.repository.AnalyticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {


    @Binds
    @Singleton
    abstract fun bindsAnalyticsDao(analyticsDaoMock: AnalyticsDaoMock): AnalyticsDao


    @Binds
    @Singleton
    abstract fun bindsAnalyticsRepository(analyticsRepositoryImpl: AnalyticsRepositoryImpl): AnalyticsRepository

}