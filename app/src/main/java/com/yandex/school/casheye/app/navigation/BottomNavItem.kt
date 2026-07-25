package com.yandex.school.casheye.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.yandex.school.casheye.R

data class BottomNavItem(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
)

val TOP_LEVEL_DESTINATIONS =
    mapOf<Route, BottomNavItem>(
        Route.Expenses to BottomNavItem(iconRes = R.drawable.receipt, titleRes = R.string.nav_expenses),
        Route.Income to BottomNavItem(iconRes = R.drawable.trending_up, titleRes = R.string.nav_income),
        Route.Account to BottomNavItem(iconRes = R.drawable.user, titleRes = R.string.nav_accounts),
    )
