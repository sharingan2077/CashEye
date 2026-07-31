package com.yandex.school.casheye.core.designsystem.component.datepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.R
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.component.ListItemDefaults
import com.yandex.school.casheye.core.model.DatePeriod
import com.yandex.school.casheye.core.model.DatePeriodPreset
import com.yandex.school.casheye.core.model.resolve
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePeriodPickerSheet(
    period: DatePeriod,
    today: LocalDate,
    onDismiss: () -> Unit,
    onPeriodSelect: (DatePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectingCustom by remember { mutableStateOf(false) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectableDates = rememberPastOrPresentSelectableDates(today)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, modifier = modifier) {
        if (selectingCustom) {
            val pickerState =
                rememberDateRangePickerState(
                    initialSelectedStartDateMillis = period.startDate.toEpochDay() * MILLIS_PER_DAY,
                    initialSelectedEndDateMillis = period.endDate.toEpochDay() * MILLIS_PER_DAY,
                    selectableDates = selectableDates,
                )
            Text(
                text = stringResource(R.string.date_period_custom),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            DateRangePicker(state = pickerState, modifier = Modifier.weight(1f, fill = false))
            Button(
                enabled = pickerState.selectedStartDateMillis != null && pickerState.selectedEndDateMillis != null,
                onClick = {
                    val start = pickerState.selectedStartDateMillis!!.toLocalDate()
                    val end = pickerState.selectedEndDateMillis!!.toLocalDate()
                    if (start <= end && end <= today) onPeriodSelect(DatePeriod(start, end))
                },
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            ) { Text(stringResource(R.string.finance_editor_apply)) }
        } else {
            Text(
                text = stringResource(R.string.date_period_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            DatePeriodPreset.entries.forEachIndexed { index, preset ->
                ListItem(
                    modifier =
                        Modifier.clickable {
                            if (preset == DatePeriodPreset.Custom) {
                                selectingCustom = true
                            } else {
                                onPeriodSelect(preset.resolve(today))
                            }
                        },
                    minHeight = ListItemDefaults.CompactMinHeight,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                ) { Text(stringResource(preset.titleRes())) }
                if (index != DatePeriodPreset.entries.lastIndex) HorizontalDivider()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun DatePeriodPreset.titleRes(): Int =
    when (this) {
        DatePeriodPreset.Today -> R.string.date_period_today
        DatePeriodPreset.Week -> R.string.date_period_week
        DatePeriodPreset.Month -> R.string.date_period_month
        DatePeriodPreset.Quarter -> R.string.date_period_quarter
        DatePeriodPreset.Year -> R.string.date_period_year
        DatePeriodPreset.Custom -> R.string.date_period_custom
    }

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private const val MILLIS_PER_DAY = 86_400_000L
