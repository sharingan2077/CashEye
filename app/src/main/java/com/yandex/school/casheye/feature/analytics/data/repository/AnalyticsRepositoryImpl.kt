package com.yandex.school.casheye.feature.analytics.data.repository

import com.yandex.school.casheye.feature.analytics.data.local.AnalyticsDao
import com.yandex.school.casheye.feature.analytics.domain.model.Analytics
import com.yandex.school.casheye.feature.analytics.domain.repository.AnalyticsRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class AnalyticsRepositoryImpl @Inject constructor(
    private val dao: AnalyticsDao
) : AnalyticsRepository {

    override fun observeAnalytics(): Flow<Analytics> {
        return dao.observeAnalytics()
    }
}