package com.yandex.school.casheye.domain.analytics.repository

import com.yandex.school.casheye.domain.analytics.model.Analytics
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {

    fun observeAnalytics(): Flow<Analytics>

}