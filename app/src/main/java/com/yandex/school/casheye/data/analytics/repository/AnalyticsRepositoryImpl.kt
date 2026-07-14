package com.yandex.school.casheye.data.analytics.repository

import com.yandex.school.casheye.data.analytics.local.AnalyticsDao
import com.yandex.school.casheye.domain.analytics.model.Analytics
import com.yandex.school.casheye.domain.analytics.repository.AnalyticsRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class AnalyticsRepositoryImpl @Inject constructor(
    private val dao: AnalyticsDao
) : AnalyticsRepository {

    override fun observeAnalytics(): Flow<Analytics> {
        return dao.observeAnalytics()
    }
}