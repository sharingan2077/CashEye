package com.yandex.school.casheye.feature.analytics.presentation

import com.yandex.school.casheye.R

sealed interface AnalyticsFilterUi {
    val resId: Int
    val title: String


    data class Type(
        override val resId: Int = R.drawable.list,
        override val title: String = "Тип",
        val type: String
    ) : AnalyticsFilterUi

    data class Period(
        override val resId: Int = R.drawable.calendar,
        override val title: String = "Период",
        val period: String
    ) : AnalyticsFilterUi

    data class Articles(
        override val resId: Int = R.drawable.tag,
        override val title: String = "Статьи",
        val articles: List<String>
    ) : AnalyticsFilterUi

    data class Account(
        override val resId: Int = R.drawable.credit_card,
        override val title: String = "Счёт",
        val accounts: List<String>
    ) : AnalyticsFilterUi

}
