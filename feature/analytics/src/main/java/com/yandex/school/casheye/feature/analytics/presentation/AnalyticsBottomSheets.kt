@file:Suppress("TooManyFunctions")

package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.EmojiCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.feature.analytics.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsModalBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        content = content,
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .padding(top = 12.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp)),
                )
            }
        },
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
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SheetTitle(title = stringResource(R.string.filter_type))
            AnalyticsType.entries.forEachIndexed { index, type ->
                SelectionRow(
                    title = type.title(),
                    selected = sheet.selected == type,
                    isLast = index == AnalyticsType.entries.lastIndex,
                    onClick = { onIntent(AnalyticsIntent.SelectDraftType(type)) },
                )
            }
            SheetButton(
                title = stringResource(R.string.done),
                paddingValues = PaddingValues(top = 16.dp, bottom = 26.dp, start = 16.dp, end = 16.dp),
                height = 52.dp,
            ) { onIntent(AnalyticsIntent.ApplyDraftType) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSheet(
    period: AnalyticsPeriod,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle(stringResource(R.string.filter_period))
        AnalyticsPeriodPreset.entries.forEachIndexed { index, preset ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onIntent(AnalyticsIntent.SelectPeriodPreset(preset))
                        }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.title(),
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
                if (preset == period.preset) {
                    Icon(
                        painter = painterResource(R.drawable.check_purple),
                        contentDescription = stringResource(R.string.selected),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (index != AnalyticsPeriodPreset.entries.lastIndex) HorizontalDivider()
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

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
        SheetTitle(stringResource(R.string.filter_categories))
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
            itemsIndexed(items = categories, key = { _, category -> category.id }) { index, category ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onIntent(AnalyticsIntent.ToggleDraftCategory(category.id))
                            }.padding(horizontal = 20.dp, vertical = 8.dp),
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
                if (index != categories.lastIndex) HorizontalDivider()
            }
        }
        SheetButton(
            title = stringResource(R.string.apply),
            paddingValues = PaddingValues(top = 20.dp, bottom = 40.dp, start = 20.dp, end = 20.dp),
        ) { onIntent(AnalyticsIntent.ApplyDraftCategories) }
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
        SheetTitle(stringResource(R.string.filter_account))
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
                    title = stringResource(R.string.all_accounts),
                    emoji = "💳",
                    subtitle = null,
                    isLast = false,
                    selected = data.filters.accountId == null,
                ) { onIntent(AnalyticsIntent.SelectAccount(null)) }
            }
            itemsIndexed(data.accounts, key = { _, account -> account.id }) { index, account ->
                AccountRow(
                    title = account.name,
                    emoji = account.emoji,
                    subtitle = null,
                    isLast = index == data.accounts.lastIndex,
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
        SheetTitle(
            title = stringResource(R.string.details),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            paddingValues = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
        )
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
                    paddingValues = PaddingValues(bottom = 32.dp),
                    showLegend = false,
                )
            }
            items(state.categorySummaries, key = { it.category.id }) { summary ->
                DetailsCategoryRow(summary = summary, total = state.total, currencyCode = state.currencyCode)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailsCategoryRow(
    summary: AnalyticsCategorySummary,
    total: BigDecimal,
    currencyCode: String,
) {
    val fraction =
        if (total.signum() == 0) {
            BigDecimal.ZERO
        } else {
            summary.amount.divide(total, 4, RoundingMode.HALF_UP)
        }
    val percentage = fraction.movePointRight(2).setScale(1, RoundingMode.HALF_UP)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(
                modifier =
                    Modifier
                        .size(12.dp)
                        .background(analyticsColorForCategory(summary.category.id), CircleShape),
            )
            Text(
                text = summary.category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = formatAmount(summary.amount, currencyCode), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "(${percentage.toPlainString()}%)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val progressShape = RoundedCornerShape(100.dp)
        LinearProgressIndicator(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(progressShape),
            progress = { fraction.toFloat() },
            color = analyticsColorForCategory(summary.category.id),
            trackColor = MaterialTheme.colorScheme.outline,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun AccountRow(
    title: String,
    emoji: String,
    subtitle: String?,
    selected: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier,
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
            trail = { AccountSelectionIndicator(selected = selected) },
            height = 64.dp,
        )
        if (!isLast) HorizontalDivider()
    }
}

@Composable
private fun SelectionRow(
    title: String,
    selected: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, modifier = Modifier.weight(1f))
            TypeSelectionIndicator(selected = selected)
        }
        if (!isLast) HorizontalDivider()
    }
}

@Composable
private fun TypeSelectionIndicator(selected: Boolean) {
    val transition = updateTransition(targetState = selected, label = "type_selection")
    val containerColor by
        transition.animateColor(
            transitionSpec = { tween(durationMillis = 200, easing = FastOutSlowInEasing) },
            label = "container_color",
        ) { isSelected ->
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        }
    val outlineColor by
        transition.animateColor(
            transitionSpec = { tween(durationMillis = 200, easing = FastOutSlowInEasing) },
            label = "outline_color",
        ) { isSelected ->
            if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline
        }
    val checkAlpha by
        transition.animateFloat(
            transitionSpec = { tween(durationMillis = 200, easing = FastOutSlowInEasing) },
            label = "check_alpha",
        ) { isSelected ->
            if (isSelected) 1f else 0f
        }
    val checkScale by
        transition.animateFloat(
            transitionSpec = { tween(durationMillis = 200, easing = FastOutSlowInEasing) },
            label = "check_scale",
        ) { isSelected ->
            if (isSelected) 1f else 0.7f
        }
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .background(containerColor, CircleShape)
                .border(width = 2.dp, color = outlineColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.check),
            contentDescription = if (selected) stringResource(R.string.selected) else null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.graphicsLayer(alpha = checkAlpha, scaleX = checkScale, scaleY = checkScale),
        )
    }
}

@Composable
private fun AccountSelectionIndicator(selected: Boolean) {
    Box(
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.check_purple),
                contentDescription = stringResource(R.string.selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SheetTitle(
    title: String,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier.padding(paddingValues),
    )
}

@Composable
private fun SheetButton(
    title: String,
    paddingValues: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    height: Dp = 48.dp,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .height(height),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun AnalyticsPeriod.formatted(): String {
    val locale = LocalConfiguration.current.locales[0] ?: LocalLocale.current.platformLocale
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale) }
    return "${startDate.format(formatter)} – ${endDate.format(formatter)}"
}

@Composable
private fun AnalyticsType.title(): String =
    stringResource(
        when (this) {
            AnalyticsType.Expenses -> R.string.type_expenses
            AnalyticsType.Income -> R.string.type_income
            AnalyticsType.All -> R.string.type_all
        },
    )

@Composable
private fun AnalyticsPeriodPreset.title(): String =
    stringResource(
        when (this) {
            AnalyticsPeriodPreset.Custom -> R.string.period_custom
            AnalyticsPeriodPreset.Week -> R.string.period_week
            AnalyticsPeriodPreset.Month -> R.string.period_month
            AnalyticsPeriodPreset.Quarter -> R.string.period_quarter
            AnalyticsPeriodPreset.Year -> R.string.period_year
        },
    )
