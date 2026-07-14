package com.yandex.school.casheye.feature.analytics.domain.repository

import com.yandex.school.casheye.feature.analytics.domain.model.Analytics
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {

    fun observeAnalytics(): Flow<Analytics>

}