package com.yandex.school.casheye.feature.analytics.presentation.sheet

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.EmojiCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.component.editor.rememberSheetListGestureCoordinator
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.feature.analytics.R
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsIntent
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsPeriod
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsPeriodPreset
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsScreenData
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsSheet
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TypeSheet(
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
internal fun PeriodSheet(
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
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (preset == AnalyticsPeriodPreset.Custom && period.preset == preset) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = period.formatted(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (preset == period.preset) {
                    Icon(
                        painter = painterResource(R.drawable.check_purple),
                        contentDescription = stringResource(R.string.period_selected),
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
internal fun CategoriesSheet(
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
                        style = MaterialTheme.typography.titleMedium,
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
internal fun AccountSheet(
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
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
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
            contentDescription = if (selected) stringResource(R.string.period_selected) else null,
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
                contentDescription = stringResource(R.string.period_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
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
