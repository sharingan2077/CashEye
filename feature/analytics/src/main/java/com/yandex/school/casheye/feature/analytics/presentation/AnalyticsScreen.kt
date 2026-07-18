package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.component.EmojiCircle
import com.yandex.school.casheye.core.designsystem.component.FilterItem
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.feature.analytics.R
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onIntent: (AnalyticsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is AnalyticsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is AnalyticsUiState.Content -> AnalyticsContent(state, onIntent)
            is AnalyticsUiState.Empty -> AnalyticsEmpty(state, onIntent)
            is AnalyticsUiState.Error -> AnalyticsError(state.message) { onIntent(AnalyticsIntent.Retry) }
        }
    }
    AnalyticsBottomSheet(state = state, onIntent = onIntent)
}

@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Content,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AnalyticsPieChart(
                total = formatAmount(state.total, state.currencyCode),
                categories = state.categorySummaries,
                modifier =
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = { onIntent(AnalyticsIntent.OpenDetails) },
                    ),
            )
        }
        item { FilterView(data = state.data, onIntent = onIntent) }
        item { TransactionsHeading() }
        items(items = state.transactions, key = Transaction::id) { transaction ->
            TransactionItem(
                emoji = transaction.category.emoji,
                title = transaction.category.name,
                comment = transaction.comment,
                amount = formatAmount(transaction.amount, state.currencyCode),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun AnalyticsEmpty(
    state: AnalyticsUiState.Empty,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterView(data = state.data, onIntent = onIntent)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "За выбранный период операций нет", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AnalyticsError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
private fun TransactionsHeading() {
    Text(
        text = "Транзакции",
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
    )
}

@Composable
internal fun TransactionItem(
    emoji: String,
    title: String,
    comment: String?,
    amount: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        lead = { EmojiCircle(emoji = emoji) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                comment?.takeIf(String::isNotBlank)?.let { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trail = {
            Text(text = amount, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
        },
    )
}

@Composable
private fun FilterView(
    data: AnalyticsScreenData,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    val filters = data.filters
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        AnalyticsFilterItem(
            iconPainter = painterResource(R.drawable.list),
            title = "Тип",
            value = filters.type.title,
            onClick = { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Type)) },
        )
        HorizontalDivider()
        AnalyticsFilterItem(
            iconPainter = painterResource(R.drawable.calendar),
            title = "Период",
            value = filters.period.formatted(),
            onClick = { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Period)) },
        )
        HorizontalDivider()
        AnalyticsFilterItem(
            iconPainter = painterResource(R.drawable.tag),
            title = "Статьи",
            value = categoriesTitle(filters.categoryIds, data.categories),
            onClick = { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Categories)) },
        )
        HorizontalDivider()
        AnalyticsFilterItem(
            iconPainter = painterResource(R.drawable.credit_card),
            title = "Счёт",
            value = data.accounts.firstOrNull { it.id == filters.accountId }?.name ?: "Все счета",
            onClick = { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Account)) },
        )
    }
}

@Composable
private fun AnalyticsFilterItem(
    iconPainter: Painter,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        lead = { IconCircle(iconPainter = iconPainter, contentDescription = title) },
        content = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        trail = { FilterItem(title = value) },
        height = 56.dp,
    )
}

@Composable
private fun IconCircle(
    iconPainter: Painter,
    contentDescription: String,
) {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = iconPainter, contentDescription = contentDescription)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsBottomSheet(
    state: AnalyticsUiState,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    when (val sheet = state.data.activeSheet) {
        null -> {
            Unit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsModalBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        content = content,
    )
}

private class SheetDragBlockingNestedScrollConnection(
    private val listState: LazyListState,
    private val listFlingBehavior: FlingBehavior,
) : NestedScrollConnection,
    FlingBehavior {
    private var userGestureInProgress = false
    private var blockSheetForGesture = true

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (
            source == NestedScrollSource.UserInput &&
            !userGestureInProgress &&
            available.y != 0f
        ) {
            userGestureInProgress = true
            blockSheetForGesture = listState.canScrollBackward || available.y < 0f
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (source != NestedScrollSource.UserInput || blockSheetForGesture) {
            Offset(x = 0f, y = available.y)
        } else {
            Offset.Zero
        }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        val consumedVelocity =
            if (userGestureInProgress && !blockSheetForGesture) {
                Velocity.Zero
            } else {
                Velocity(x = 0f, y = available.y)
            }
        userGestureInProgress = false
        blockSheetForGesture = true
        return consumedVelocity
    }

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (userGestureInProgress && !blockSheetForGesture) return initialVelocity

        val scrollScope = this
        return with(listFlingBehavior) {
            scrollScope.performFling(initialVelocity)
        }
    }
}

@Composable
private fun rememberSheetListGestureCoordinator(listState: LazyListState): SheetDragBlockingNestedScrollConnection {
    val listFlingBehavior = ScrollableDefaults.flingBehavior()
    return remember(listState, listFlingBehavior) {
        SheetDragBlockingNestedScrollConnection(
            listState = listState,
            listFlingBehavior = listFlingBehavior,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSheet(
    sheet: AnalyticsSheet.Type,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Тип")
        AnalyticsType.entries.forEach { type ->
            SelectionRow(
                title = type.title,
                selected = sheet.selected == type,
                onClick = { onIntent(AnalyticsIntent.SelectDraftType(type)) },
            )
        }
        SheetButton("Готово") { onIntent(AnalyticsIntent.ApplyDraftType) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSheet(
    period: AnalyticsPeriod,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Период")
        AnalyticsPeriodPreset.entries.forEach { preset ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onIntent(AnalyticsIntent.SelectPeriodPreset(preset))
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.title,
                        fontWeight = FontWeight.Medium,
                    )
                    if (preset == AnalyticsPeriodPreset.Custom && period.preset == preset) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = period.formatted(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (preset == period.preset) Text(text = "✓", color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomPeriodSheet(
    sheet: AnalyticsSheet.CustomPeriod,
    currentDate: LocalDate,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    var selectedStartDate by remember(sheet.startDate) { mutableStateOf(sheet.startDate) }
    var selectedEndDate by remember(sheet.endDate) { mutableStateOf(sheet.endDate) }
    val currentMonth = remember(currentDate) { YearMonth.from(currentDate) }
    val latestFirstMonth = remember(currentMonth) { currentMonth.minusMonths(1) }
    var firstVisibleMonth by
    remember(sheet.startDate, currentDate) {
        val selectedMonth = sheet.startDate?.let(YearMonth::from) ?: latestFirstMonth
        mutableStateOf(minOf(selectedMonth, latestFirstMonth))
    }
    val valid = selectedStartDate != null && selectedEndDate != null && selectedEndDate!! <= currentDate

    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        Text(
            text = "Произвольный период",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 14.dp),
        )
        DateRangeFields(
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
        ) {
            item {
                CalendarMonthHeader(
                    month = firstVisibleMonth,
                    canNavigateForward = firstVisibleMonth < latestFirstMonth,
                    onPrevious = { firstVisibleMonth = firstVisibleMonth.minusMonths(1) },
                    onNext = { firstVisibleMonth = firstVisibleMonth.plusMonths(1) },
                )
            }
            item { CalendarWeekdays(modifier = Modifier.padding(horizontal = 44.dp)) }
            item {
                CalendarMonthGrid(
                    month = firstVisibleMonth,
                    currentDate = currentDate,
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    onDateClick = { date ->
                        val selection = selectRangeDate(date, selectedStartDate, selectedEndDate)
                        selectedStartDate = selection.first
                        selectedEndDate = selection.second
                    },
                    modifier = Modifier.padding(horizontal = 44.dp),
                )
            }
            item {
                Text(
                    text = firstVisibleMonth.plusMonths(1).formattedMonth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
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
                    onDateClick = { date ->
                        val selection = selectRangeDate(date, selectedStartDate, selectedEndDate)
                        selectedStartDate = selection.first
                        selectedEndDate = selection.second
                    },
                    modifier = Modifier.padding(horizontal = 44.dp),
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onIntent(AnalyticsIntent.DismissSheet) }) { Text("Отмена") }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                enabled = valid,
                shape = RoundedCornerShape(20.dp),
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier.height(40.dp).width(124.dp),
                onClick = {
                    onIntent(
                        AnalyticsIntent.UpdateCustomPeriod(
                            selectedStartDate,
                            selectedEndDate,
                        ),
                    )
                    onIntent(AnalyticsIntent.ApplyCustomPeriod)
                },
            ) { Text("Применить") }
        }
    }
}

@Composable
private fun DateRangeFields(
    startDate: LocalDate?,
    endDate: LocalDate?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DateField(startDate, Modifier.weight(1f))
        Text(
            text = "–",
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp),
        )
        DateField(endDate, Modifier.weight(1f))
    }
}

@Composable
private fun DateField(
    date: LocalDate?,
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
            text = date?.format(CALENDAR_DATE_FORMATTER).orEmpty(),
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarNavigationButton(pointsRight = false, enabled = true, onClick = onPrevious)
        Text(
            text = month.formattedMonth(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
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
                .alpha(if (enabled) 1f else 0.38f)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val xStart = if (pointsRight) size.width * 0.35f else size.width * 0.65f
            val xEnd = if (pointsRight) size.width * 0.65f else size.width * 0.35f
            drawLine(
                color = color,
                start = Offset(xStart, size.height * 0.2f),
                end = Offset(xEnd, size.height * 0.5f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(xEnd, size.height * 0.5f),
                end = Offset(xStart, size.height * 0.8f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun CalendarWeekdays(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        CALENDAR_WEEKDAYS.forEach { day ->
            Text(
                text = day,
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
    val numberOfWeeks = (firstDayOffset + month.lengthOfMonth() + 6) / 7
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(numberOfWeeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - firstDayOffset + 1
                    if (day in 1..month.lengthOfMonth()) {
                        val date = month.atDay(day)
                        CalendarDay(
                            date = date,
                            enabled = date <= currentDate,
                            isStart = date == selectedStartDate,
                            isEnd = date == selectedEndDate,
                            isInRange =
                                selectedStartDate != null &&
                                    selectedEndDate != null &&
                                    date > selectedStartDate &&
                                    date < selectedEndDate,
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
                        isStart && isEnd -> Unit
                        isStart && hasRangeAfter -> {
                            drawRect(rangeColor, topLeft = Offset(size.width / 2f, 0f))
                        }

                        isEnd && hasRangeBefore -> {
                            drawRect(rangeColor, size = Size(size.width / 2f, size.height))
                        }
                    }
                    if (isStart || isEnd) {
                        drawCircle(selectionColor, radius = size.minDimension / 2f)
                    }
                }
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color =
                when {
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
): Pair<LocalDate, LocalDate?> =
    when {
        selectedStartDate == null || selectedEndDate != null -> date to null
        date < selectedStartDate -> date to null
        else -> selectedStartDate to date
    }

private fun YearMonth.formattedMonth(): String =
    atDay(1).format(CALENDAR_MONTH_FORMATTER).replaceFirstChar { it.titlecase(CALENDAR_LOCALE) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesSheet(
    sheet: AnalyticsSheet.Categories,
    categories: List<Category>,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val gestureCoordinator = rememberSheetListGestureCoordinator(listState)
    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Статьи")
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .nestedScroll(gestureCoordinator),
            state = listState,
            flingBehavior = gestureCoordinator,
            overscrollEffect = null,
        ) {
            items(categories, key = Category::id) { category ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onIntent(AnalyticsIntent.ToggleDraftCategory(category.id))
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EmojiCircle(emoji = category.emoji)
                    Text(
                        text = category.name,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                    )
                    Checkbox(
                        checked = category.id in sheet.selectedIds,
                        onCheckedChange = { onIntent(AnalyticsIntent.ToggleDraftCategory(category.id)) },
                    )
                }
                HorizontalDivider()
            }
        }
        SheetButton("Применить") { onIntent(AnalyticsIntent.ApplyDraftCategories) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(
    data: AnalyticsScreenData,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val gestureCoordinator = rememberSheetListGestureCoordinator(listState)
    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Счёт")
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .nestedScroll(gestureCoordinator),
            state = listState,
            flingBehavior = gestureCoordinator,
            overscrollEffect = null,
        ) {
            item {
                AccountRow(
                    title = "Все счета",
                    emoji = "💳",
                    subtitle = null,
                    selected = data.filters.accountId == null,
                ) { onIntent(AnalyticsIntent.SelectAccount(null)) }
            }
            items(data.accounts, key = Account::id) { account ->
                AccountRow(
                    title = account.name,
                    emoji = account.emoji,
                    subtitle = null,
                    selected = data.filters.accountId == account.id,
                ) { onIntent(AnalyticsIntent.SelectAccount(account.id)) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSheet(
    state: AnalyticsUiState.Content,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val gestureCoordinator = rememberSheetListGestureCoordinator(listState)
    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Детализация")
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .nestedScroll(gestureCoordinator),
            state = listState,
            flingBehavior = gestureCoordinator,
            overscrollEffect = null,
        ) {
            item {
                AnalyticsPieChart(
                    total = formatAmount(state.total, state.currencyCode),
                    categories = state.categorySummaries,
                    showLegend = false,
                )
            }
            items(state.categorySummaries, key = { it.category.id }) { summary ->
                val fraction =
                    if (state.total.signum() == 0) {
                        0f
                    } else {
                        summary.amount.divide(state.total, 4, RoundingMode.HALF_UP).toFloat()
                    }
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "●",
                            color = analyticsColorForCategory(summary.category.id),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(text = summary.category.name, modifier = Modifier.weight(1f))
                        Text(
                            text = "${formatAmount(summary.amount, state.currencyCode)} (${(fraction * 100).toInt()}%)",
                        )
                    }
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        color = analyticsColorForCategory(summary.category.id),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AccountRow(
    title: String,
    emoji: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        lead = { EmojiCircle(emoji = emoji) },
        content = {
            Column {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        trail = { RadioButton(selected = selected, onClick = onClick) },
        height = 64.dp,
    )
    HorizontalDivider()
}

@Composable
private fun SelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
    HorizontalDivider()
}

@Composable
private fun SheetTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp))
}

@Composable
private fun SheetButton(
    title: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) { Text(title) }
}

private fun AnalyticsPeriod.formatted(): String = "$startDateFormatted – $endDateFormatted"

private val AnalyticsPeriod.startDateFormatted: String
    get() = startDate.format(DATE_FORMATTER)

private val AnalyticsPeriod.endDateFormatted: String
    get() = endDate.format(DATE_FORMATTER)

private fun categoriesTitle(
    selectedIds: Set<Int>,
    categories: List<Category>,
): String {
    if (selectedIds.isEmpty()) return "Все статьи"
    val names = categories.filter { it.id in selectedIds }.map { it.name }
    return when {
        names.isEmpty() -> "Выбрано: ${selectedIds.size}"
        names.size <= 2 -> names.joinToString()
        else -> "Выбрано: ${selectedIds.size}"
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val CALENDAR_LOCALE: Locale = Locale.forLanguageTag("ru")
private val CALENDAR_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM. yyyy", CALENDAR_LOCALE)
private val CALENDAR_MONTH_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("LLLL yyyy", CALENDAR_LOCALE)
private val CALENDAR_WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
