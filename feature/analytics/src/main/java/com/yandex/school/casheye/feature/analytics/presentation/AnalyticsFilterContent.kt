package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.FilterItem
import com.yandex.school.casheye.core.designsystem.component.IconCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.component.ListItemDefaults
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.feature.analytics.R
import com.yandex.school.casheye.feature.analytics.presentation.sheet.formatted

@Composable
internal fun AnalyticsFilterContent(
    data: AnalyticsScreenData,
    onIntent: (AnalyticsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = data.filters
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        AnalyticsFilterItem(
            painterResource(R.drawable.list),
            stringResource(R.string.filter_type),
            filters.type.title(),
        ) {
            onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Type))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AnalyticsFilterItem(
            painterResource(R.drawable.calendar),
            stringResource(R.string.filter_period),
            filters.period.formatted(),
        ) {
            onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Period))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AnalyticsFilterItem(
            painterResource(R.drawable.tag),
            stringResource(R.string.filter_categories),
            categoriesTitle(filters.categoryIds, data.categories),
        ) {
            onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Categories))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AnalyticsFilterItem(
            painterResource(R.drawable.credit_card),
            stringResource(R.string.filter_account),
            data.accounts.firstOrNull { it.id == filters.accountId }?.name ?: stringResource(R.string.all_accounts),
        ) { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Account)) }
    }
}

@Composable
private fun AnalyticsFilterItem(
    iconPainter: Painter,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    ListItem(modifier = Modifier.clickable(onClick = onClick), leadingContent = {
        IconCircle(iconPainter = iconPainter, contentDescription = title, iconSize = 18.dp)
    }, content = {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }, trailingContent = { FilterItem(title = value) }, minHeight = ListItemDefaults.CompactMinHeight)
}

@Composable
private fun categoriesTitle(
    selectedIds: Set<Int>,
    categories: List<Category>,
): String {
    if (selectedIds.isEmpty()) return stringResource(R.string.all_categories)
    val names = categories.filter { it.id in selectedIds }.map { it.name }
    return when {
        names.isEmpty() -> stringResource(R.string.selected_count, selectedIds.size)
        names.size <= 2 -> names.joinToString()
        else -> stringResource(R.string.selected_count, selectedIds.size)
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
