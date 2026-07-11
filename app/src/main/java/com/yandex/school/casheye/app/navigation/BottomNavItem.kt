package com.yandex.school.casheye.app.navigation

import androidx.annotation.DrawableRes
import com.yandex.school.casheye.R

data class BottomNavItem(
    @param:DrawableRes val iconRes: Int,
    val title: String
)


val TOP_LEVEL_DESTINATIONS = mapOf<Route, BottomNavItem>(
    Route.Expenses to BottomNavItem(iconRes = R.drawable.receipt, title = "Расходы"),
    Route.Income to BottomNavItem(iconRes = R.drawable.trending_up, title = "Доходы"),
    Route.Account to BottomNavItem(iconRes = R.drawable.user, title = "Счета")

)
