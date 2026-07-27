package com.yandex.school.casheye.feature.analytics.presentation.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsIntent
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsSheet
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsUiState
import com.yandex.school.casheye.feature.analytics.presentation.calendar.CustomPeriodSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsBottomSheet(
    state: AnalyticsUiState,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    when (val sheet = state.data.activeSheet) {
        null -> {
        }

        is AnalyticsSheet.Type -> {
            TypeSheet(sheet, onIntent)
        }

        AnalyticsSheet.Period -> {
            PeriodSheet(state.data.filters.period, onIntent)
        }

        is AnalyticsSheet.CustomPeriod -> {
            CustomPeriodSheet(sheet, state.data.currentDate, onIntent)
        }

        is AnalyticsSheet.Categories -> {
            CategoriesSheet(sheet, state.data.categories, onIntent)
        }

        AnalyticsSheet.Account -> {
            AccountSheet(state.data, onIntent)
        }

        AnalyticsSheet.Details -> {
            if (state is AnalyticsUiState.Content) DetailsSheet(state, onIntent)
        }
    }
}
