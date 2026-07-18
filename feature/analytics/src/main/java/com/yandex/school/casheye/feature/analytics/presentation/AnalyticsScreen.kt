package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
            PeriodSheet(state.data.filters.period.preset, onIntent)
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
private fun TypeSheet(
    sheet: AnalyticsSheet.Type,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
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
    selectedPreset: AnalyticsPeriodPreset,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Период")
        AnalyticsPeriodPreset.entries.forEach { preset ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onIntent(AnalyticsIntent.SelectPeriodPreset(preset))
                        }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = preset.title, modifier = Modifier.weight(1f))
                if (preset == selectedPreset) Text(text = "✓", color = MaterialTheme.colorScheme.primary)
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
    val todayMillis = currentDate.toEpochMillis()
    val selectableDates =
        remember(todayMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
            }
        }
    val pickerState =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = sheet.startDate?.toEpochMillis(),
            initialSelectedEndDateMillis = sheet.endDate?.toEpochMillis(),
            selectableDates = selectableDates,
        )
    val valid =
        pickerState.selectedStartDateMillis != null &&
            pickerState.selectedEndDateMillis != null &&
            pickerState.selectedEndDateMillis!! <= todayMillis
    ModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Произвольный период")
        DateRangePicker(
            state = pickerState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
            colors = DatePickerDefaults.colors(),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { onIntent(AnalyticsIntent.DismissSheet) }) { Text("Отмена") }
            Spacer(modifier = Modifier.size(8.dp))
            Button(
                enabled = valid,
                onClick = {
                    onIntent(
                        AnalyticsIntent.UpdateCustomPeriod(
                            pickerState.selectedStartDateMillis?.toLocalDate(),
                            pickerState.selectedEndDateMillis?.toLocalDate(),
                        ),
                    )
                    onIntent(AnalyticsIntent.ApplyCustomPeriod)
                },
            ) { Text("Применить") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesSheet(
    sheet: AnalyticsSheet.Categories,
    categories: List<Category>,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Статьи")
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
        ) {
            items(categories, key = Category::id) { category ->
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
    ModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Счёт")
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
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
    ModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle("Детализация")
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp),
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

private fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
