package com.yandex.school.casheye.feature.analytics.data.local

import com.yandex.school.casheye.feature.analytics.domain.model.Analytics
import kotlinx.coroutines.flow.Flow

interface AnalyticsDao {

    fun observeAnalytics(): Flow<Analytics>

}