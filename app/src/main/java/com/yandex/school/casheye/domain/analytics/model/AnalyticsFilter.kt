package com.yandex.school.casheye.domain.analytics.model

sealed interface AnalyticsFilter {
    data class Type(val type: String) : AnalyticsFilter

    data class Period(val period: String) : AnalyticsFilter

    data class Articles(val articles: List<String>) : AnalyticsFilter

    data class Account(val accounts: List<String>) : AnalyticsFilter
}
