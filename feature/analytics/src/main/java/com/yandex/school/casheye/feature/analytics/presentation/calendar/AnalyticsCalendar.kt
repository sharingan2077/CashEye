package com.yandex.school.casheye.feature.analytics.presentation.calendar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.school.casheye.core.designsystem.component.datepicker.CustomDateRangePickerContent
import com.yandex.school.casheye.feature.analytics.R
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsIntent
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsSheet
import com.yandex.school.casheye.feature.analytics.presentation.sheet.AnalyticsModalBottomSheet
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomPeriodSheet(
    sheet: AnalyticsSheet.CustomPeriod,
    currentDate: LocalDate,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        CustomDateRangePickerContent(
            initialStartDate = sheet.startDate,
            initialEndDate = sheet.endDate,
            currentDate = currentDate,
            title = stringResource(R.string.period_custom),
            cancelLabel = stringResource(R.string.cancel),
            applyLabel = stringResource(R.string.apply),
            onCancel = { onIntent(AnalyticsIntent.DismissSheet) },
            onApply = { startDate, endDate ->
                onIntent(AnalyticsIntent.UpdateCustomPeriod(startDate, endDate))
                onIntent(AnalyticsIntent.ApplyCustomPeriod)
            },
        )
    }
}
