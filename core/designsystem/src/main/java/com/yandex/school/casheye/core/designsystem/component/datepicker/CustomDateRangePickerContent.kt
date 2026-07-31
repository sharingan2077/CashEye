package com.yandex.school.casheye.core.designsystem.component.datepicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.component.editor.rememberSheetListGestureCoordinator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun CustomDateRangePickerContent(
    initialStartDate: LocalDate?,
    initialEndDate: LocalDate?,
    currentDate: LocalDate,
    title: String,
    cancelLabel: String,
    applyLabel: String,
    onCancel: () -> Unit,
    onApply: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedStartDate by remember(initialStartDate) { mutableStateOf(initialStartDate) }
    var selectedEndDate by remember(initialEndDate) { mutableStateOf(initialEndDate) }
    val currentMonth = remember(currentDate) { YearMonth.from(currentDate) }
    val latestFirstMonth = remember(currentMonth) { currentMonth.minusMonths(1) }
    var firstVisibleMonth by
        remember(initialStartDate, currentDate) {
            val selectedMonth = initialStartDate?.let(YearMonth::from) ?: latestFirstMonth
            mutableStateOf(minOf(selectedMonth, latestFirstMonth))
        }
    val valid = selectedStartDate != null && selectedEndDate != null && selectedEndDate!! <= currentDate
    val onDateClick: (LocalDate) -> Unit = { date ->
        val (startDate, endDate) = selectRangeDate(date, selectedStartDate, selectedEndDate)
        selectedStartDate = startDate
        selectedEndDate = endDate
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 14.dp),
        )
        DateRangeFields(
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        CalendarMonthSelector(
            firstVisibleMonth = firstVisibleMonth,
            latestFirstMonth = latestFirstMonth,
            currentDate = currentDate,
            selectedStartDate = selectedStartDate,
            selectedEndDate = selectedEndDate,
            onPreviousMonth = { firstVisibleMonth = firstVisibleMonth.minusMonths(1) },
            onNextMonth = { firstVisibleMonth = firstVisibleMonth.plusMonths(1) },
            onDateClick = onDateClick,
        )
        CustomPeriodActions(
            valid = valid,
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            cancelLabel = cancelLabel,
            applyLabel = applyLabel,
            onCancel = onCancel,
            onApply = onApply,
        )
    }
}

@Composable
private fun CalendarMonthSelector(
    firstVisibleMonth: YearMonth,
    latestFirstMonth: YearMonth,
    currentDate: LocalDate,
    selectedStartDate: LocalDate?,
    selectedEndDate: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0] ?: LocalLocale.current.platformLocale
    val listState = rememberLazyListState()
    val gestureCoordinator = rememberSheetListGestureCoordinator(listState)
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .nestedScroll(gestureCoordinator),
        state = listState,
        flingBehavior = gestureCoordinator,
        overscrollEffect = null,
    ) {
        item {
            CalendarMonthHeader(
                month = firstVisibleMonth,
                canNavigateForward = firstVisibleMonth < latestFirstMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
            )
        }
        item { CalendarWeekdays(modifier = Modifier.padding(horizontal = 44.dp)) }
        item {
            CalendarMonthGrid(
                month = firstVisibleMonth,
                currentDate = currentDate,
                selectedStartDate = selectedStartDate,
                selectedEndDate = selectedEndDate,
                onDateClick = onDateClick,
                modifier = Modifier.padding(horizontal = 44.dp),
            )
        }
        item {
            Text(
                text =
                    firstVisibleMonth
                        .plusMonths(1)
                        .atDay(1)
                        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                        .replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
            )
        }
        item { CalendarWeekdays(modifier = Modifier.padding(horizontal = 44.dp)) }
        item {
            CalendarMonthGrid(
                month = firstVisibleMonth.plusMonths(1),
                currentDate = currentDate,
                selectedStartDate = selectedStartDate,
                selectedEndDate = selectedEndDate,
                onDateClick = onDateClick,
                modifier = Modifier.padding(horizontal = 44.dp),
            )
        }
    }
}

@Composable
private fun CustomPeriodActions(
    valid: Boolean,
    startDate: LocalDate?,
    endDate: LocalDate?,
    cancelLabel: String,
    applyLabel: String,
    onCancel: () -> Unit,
    onApply: (LocalDate, LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel) { Text(cancelLabel) }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            enabled = valid,
            shape = RoundedCornerShape(20.dp),
            contentPadding = ButtonDefaults.ContentPadding,
            modifier = Modifier.height(40.dp).width(124.dp),
            onClick = { onApply(startDate!!, endDate!!) },
        ) { Text(applyLabel) }
    }
}

@Composable
private fun DateRangeFields(
    startDate: LocalDate?,
    endDate: LocalDate?,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0] ?: LocalLocale.current.platformLocale
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DateField(startDate, locale, Modifier.weight(1f))
        Text(
            text = "–",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp),
        )
        DateField(endDate, locale, Modifier.weight(1f))
    }
}

@Composable
private fun DateField(
    date: LocalDate?,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(40.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)).orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CalendarMonthHeader(
    month: YearMonth,
    canNavigateForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0] ?: LocalLocale.current.platformLocale
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarNavigationButton(pointsRight = false, enabled = true, onClick = onPrevious)
        Text(
            text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        CalendarNavigationButton(pointsRight = true, enabled = canNavigateForward, onClick = onNext)
    }
}

@Composable
private fun CalendarNavigationButton(
    pointsRight: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = MaterialTheme.colorScheme.onSurface
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .alpha(if (enabled) 1f else DISABLED_ARROW_ALPHA)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val xStart = if (pointsRight) size.width * ARROW_START_X else size.width * ARROW_END_X
            val xEnd = if (pointsRight) size.width * ARROW_END_X else size.width * ARROW_START_X
            drawLine(
                color = color,
                start = Offset(xStart, size.height * ARROW_TOP_Y),
                end = Offset(xEnd, size.height * ARROW_MIDDLE_Y),
                strokeWidth = ARROW_STROKE_WIDTH.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(xEnd, size.height * ARROW_MIDDLE_Y),
                end = Offset(xStart, size.height * ARROW_BOTTOM_Y),
                strokeWidth = ARROW_STROKE_WIDTH.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun CalendarWeekdays(modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0] ?: LocalLocale.current.platformLocale
    Row(modifier = modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    currentDate: LocalDate,
    selectedStartDate: LocalDate?,
    selectedEndDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOffset = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val numberOfWeeks = (firstDayOffset + month.lengthOfMonth() + 6) / CALENDAR_DAYS_IN_WEEK
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(numberOfWeeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(CALENDAR_DAYS_IN_WEEK) { weekday ->
                    val day = week * CALENDAR_DAYS_IN_WEEK + weekday - firstDayOffset + 1
                    if (day in 1..month.lengthOfMonth()) {
                        val date = month.atDay(day)
                        CalendarDay(
                            date = date,
                            enabled = date <= currentDate,
                            isStart = date == selectedStartDate,
                            isEnd = date == selectedEndDate,
                            isInRange = selectedStartDate != null && selectedEndDate != null && date > selectedStartDate && date < selectedEndDate,
                            hasRangeBefore = selectedStartDate != null && date > selectedStartDate,
                            hasRangeAfter = selectedEndDate != null && date < selectedEndDate,
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    enabled: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isInRange: Boolean,
    hasRangeBefore: Boolean,
    hasRangeAfter: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rangeColor = MaterialTheme.colorScheme.primaryContainer
    val selectionColor = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            modifier
                .height(40.dp)
                .drawBehind {
                    when {
                        isInRange -> drawRect(rangeColor)
                        isStart && hasRangeAfter -> drawRect(rangeColor, topLeft = Offset(size.width / 2f, 0f))
                        isEnd && hasRangeBefore -> drawRect(rangeColor, size = Size(size.width / 2f, size.height))
                        else -> Unit
                    }
                    if (isStart || isEnd) drawCircle(selectionColor, radius = size.minDimension / 2f)
                }.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
                enabled -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
    }
}

private fun selectRangeDate(
    date: LocalDate,
    selectedStartDate: LocalDate?,
    selectedEndDate: LocalDate?,
): Pair<LocalDate?, LocalDate?> =
    when {
        selectedStartDate == null || selectedEndDate != null -> date to null
        date < selectedStartDate -> date to null
        else -> selectedStartDate to date
    }

private const val ARROW_START_X = 0.35f
private const val ARROW_END_X = 0.65f
private const val ARROW_TOP_Y = 0.2f
private const val ARROW_MIDDLE_Y = 0.5f
private const val ARROW_BOTTOM_Y = 0.8f
private val ARROW_STROKE_WIDTH = 2.dp
private const val DISABLED_ARROW_ALPHA = 0.38f
private const val CALENDAR_DAYS_IN_WEEK = 7
