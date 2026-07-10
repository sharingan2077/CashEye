package com.yandex.school.casheye.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val icon: ImageVector,
    val title: String
)


val TOP_LEVEL_DESTINATIONS = mapOf<Route, BottomNavItem>(
    Route.Expenses to BottomNavItem(icon = Icons.Default.Receipt, title = "Расходы"),
    Route.Income to BottomNavItem(icon = Icons.Default.TrendingUp, title = "Доходы"),
    Route.Account to BottomNavItem(icon = Icons.Default.VerifiedUser, title = "Счета")

)