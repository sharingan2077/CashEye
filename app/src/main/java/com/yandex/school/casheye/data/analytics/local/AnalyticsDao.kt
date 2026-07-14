package com.yandex.school.casheye.data.analytics.local

import com.yandex.school.casheye.domain.analytics.model.Analytics
import kotlinx.coroutines.flow.Flow

interface AnalyticsDao {

    fun observeAnalytics(): Flow<Analytics>

}