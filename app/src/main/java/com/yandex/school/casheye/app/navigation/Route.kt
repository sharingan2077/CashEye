package com.yandex.school.casheye.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable


@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Expenses : Route

    @Serializable
    data object Income : Route

    @Serializable
    data object Accounts : Route

    @Serializable
    data object Analytics : Route

}
