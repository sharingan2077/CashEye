package com.yandex.school.casheye.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CashEyeExtendedColors(
    val chartExpense: Color,
    val chartIncome: Color,
    val chartOther: Color,
)

internal val LightExtendedColors =
    CashEyeExtendedColors(
        chartExpense = ChartExpense,
        chartIncome = ChartIncome,
        chartOther = ChartOther,
    )

internal val DarkExtendedColors =
    CashEyeExtendedColors(
        chartExpense = ChartExpenseDark,
        chartIncome = ChartIncomeDark,
        chartOther = ChartOtherDark,
    )

internal val LocalCashEyeExtendedColors =
    staticCompositionLocalOf<CashEyeExtendedColors> {
        error("CashEyeExtendedColors are not provided")
    }

object CashEyeExtendedTheme {
    val colors: CashEyeExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCashEyeExtendedColors.current
}
