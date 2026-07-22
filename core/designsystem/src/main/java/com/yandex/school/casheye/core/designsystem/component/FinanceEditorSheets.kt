package com.yandex.school.casheye.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class EditorOption(
    val id: Int,
    val label: String,
    val emoji: String = "",
    val currencyCode: String? = null,
)

internal data class CurrencyOption(
    val code: String,
    val flag: String,
    @StringRes val titleRes: Int,
)

internal val currencies =
    listOf(
        CurrencyOption("RUB", "🇷🇺", R.string.finance_editor_currency_rub),
        CurrencyOption("USD", "🇺🇸", R.string.finance_editor_currency_usd),
        CurrencyOption("EUR", "🇪🇺", R.string.finance_editor_currency_eur),
        CurrencyOption("GBP", "🇬🇧", R.string.finance_editor_currency_gbp),
        CurrencyOption("CNY", "🇨🇳", R.string.finance_editor_currency_cny),
    )

/** Returns a compact symbol for amounts while retaining an ISO fallback for unknown currencies. */
fun currencySymbol(currencyCode: String): String =
    when (currencyCode.uppercase(Locale.ROOT)) {
        "RUB" -> "₽"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "CNY" -> "¥"
        else -> currencyCode.uppercase(Locale.ROOT)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorSheet(
    amount: String,
    category: EditorOption?,
    categories: List<EditorOption>,
    account: EditorOption?,
    accounts: List<EditorOption>,
    date: LocalDate,
    time: LocalTime,
    comment: String,
    isSaving: Boolean,
    error: String?,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (EditorOption) -> Unit,
    onAccountChange: (EditorOption) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onCommentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var nested by remember { mutableStateOf<TransactionNestedSheet?>(null) }
    val transactionIsValid = amount.isNotBlank() && category != null && account != null
    val dateFormatter = rememberEditorDateFormatter()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    FinanceEditorSheet(
        amount = amount,
        currency = currencySymbol(account?.currencyCode ?: "RUB"),
        isSaving = isSaving,
        isSaveEnabled = transactionIsValid,
        error = error,
        onAmountChange = onAmountChange,
        onSave = onSave,
        onDismiss = onDismiss,
        sheetState = sheetState,
    ) { clearPrimaryFocus ->
        EditorRow(
            icon = R.drawable.ic_editor_category,
            label = stringResource(R.string.finance_editor_category),
            value = category?.label ?: "Выбрать",
            onClick = {
                clearPrimaryFocus()
                nested = TransactionNestedSheet.Category
            },
        )
        EditorRow(
            icon = R.drawable.ic_editor_calendar,
            label = stringResource(R.string.finance_editor_date),
            value = date.format(dateFormatter),
            onClick = {
                clearPrimaryFocus()
                nested = TransactionNestedSheet.Date
            },
        )
        EditorRow(
            icon = R.drawable.ic_editor_calendar,
            label = stringResource(R.string.finance_editor_time),
            value = time.format(editorTimeFormatter),
            onClick = {
                clearPrimaryFocus()
                nested = TransactionNestedSheet.Time
            },
        )
        EditorRow(
            icon = R.drawable.ic_editor_wallet,
            label = stringResource(R.string.finance_editor_account),
            value = account?.label.orEmpty(),
            onClick = {
                clearPrimaryFocus()
                nested = TransactionNestedSheet.Account
            },
        )
        EditorRow(
            icon = R.drawable.ic_editor_comment,
            label = stringResource(R.string.finance_editor_comment),
            value = comment.ifBlank { stringResource(R.string.finance_editor_not_specified) },
            onClick = {
                clearPrimaryFocus()
                nested = TransactionNestedSheet.Comment
            },
        )
    }

    when (nested) {
        TransactionNestedSheet.Category -> {
            EditorOptionSheet(
                title = stringResource(R.string.finance_editor_categories),
                options = categories,
                selectedId = category?.id,
                searchable = true,
                onSelect = {
                    onCategoryChange(it)
                    nested = null
                },
                onDismiss = { nested = null },
            )
        }

        TransactionNestedSheet.Account -> {
            EditorAccountSheet(
                accounts = accounts,
                selectedId = account?.id,
                onSelect = {
                    onAccountChange(it)
                    nested = null
                },
                onDismiss = { nested = null },
            )
        }

        TransactionNestedSheet.Comment -> {
            EditorTextSheet(
                title = stringResource(R.string.finance_editor_comment),
                value = comment,
                singleLine = false,
                onConfirm = {
                    onCommentChange(it)
                    nested = null
                },
                onDismiss = { nested = null },
            )
        }

        TransactionNestedSheet.Date -> {
            EditorDateSheet(
                date = date,
                onConfirm = {
                    onDateChange(it)
                    nested = null
                },
                onDismiss = { nested = null },
            )
        }

        TransactionNestedSheet.Time -> {
            EditorTimeSheet(
                time = time,
                onConfirm = {
                    onTimeChange(it)
                    nested = null
                },
                onDismiss = { nested = null },
            )
        }

        null -> {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinanceEditorSheet(
    amount: String,
    currency: String,
    isSaving: Boolean,
    isSaveEnabled: Boolean,
    error: String?,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    title: String? = null,
    rows: @Composable ColumnScope.(clearPrimaryFocus: () -> Unit) -> Unit,
) {
    val amountFocusRequester = remember { FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(Unit) { amountFocusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { EditorSheetHandle() },
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding(),
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp),
                )
            }
            EditorAmountField(
                amount = amount,
                currency = currency,
                onAmountChange = onAmountChange,
                modifier = Modifier.padding(horizontal = 20.dp),
                focusRequester = amountFocusRequester,
            )
            Spacer(Modifier.height(18.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                content = { rows { focusManager.clearFocus(force = true) } },
            )
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 9.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                EditorConfirmFab(
                    enabled = isSaveEnabled && !isSaving,
                    isSaving = isSaving,
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
internal fun EditorSheetHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Spacer(
            modifier =
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
        )
    }
}

@Composable
private fun EditorAmountField(
    amount: String,
    currency: String,
    onAmountChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(min = 160.dp)
                    .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.width(IntrinsicSize.Min).focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle =
                        MaterialTheme.typography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        ),
                )
                Text(
                    text = currency,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
internal fun currencyShortLabel(currencyCode: String): String {
    val titleRes =
        when (currencyCode.uppercase(Locale.ROOT)) {
            "RUB" -> R.string.finance_editor_currency_rub_short
            "USD" -> R.string.finance_editor_currency_usd_short
            "EUR" -> R.string.finance_editor_currency_eur_short
            "GBP" -> R.string.finance_editor_currency_gbp_short
            "CNY" -> R.string.finance_editor_currency_cny_short
            else -> return currencyCode.uppercase(Locale.ROOT)
        }
    return stringResource(titleRes)
}

@Composable
internal fun EditorRow(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 16.dp),
            )
            ValuePill(value)
        }
        if (showDivider) HorizontalDivider()
    }
}

@Composable
private fun ValuePill(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        modifier =
            Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun EditorConfirmFab(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        containerColor = if (enabled) Color.Black else MaterialTheme.colorScheme.outlineVariant,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        if (isSaving) {
            Text("…", style = MaterialTheme.typography.titleLarge)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_editor_check),
                contentDescription = stringResource(R.string.finance_editor_save),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorOptionSheet(
    title: String,
    options: List<EditorOption>,
    selectedId: Int?,
    searchable: Boolean,
    onSelect: (EditorOption) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visibleOptions = remember(options, query) { options.filter { it.label.contains(query, ignoreCase = true) } }
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { EditorSheetHandle() }) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            EditorSheetTitle(title)
            if (searchable) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.finance_editor_find_category)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_editor_search),
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 20.dp, end = 20.dp),
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                itemsIndexed(visibleOptions, key = { _, option -> option.id }) { index, option ->

                    EditorSelectionRow(
                        emoji = option.emoji,
                        title = option.label,
                        subtitle = null,
                        selected = option.id == selectedId,
                        isLast = index == visibleOptions.lastIndex,
                        onClick = { onSelect(option) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
internal fun EditorSelectionRow(
    emoji: String,
    title: String,
    subtitle: String?,
    selected: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Column {
        ListItem(
            lead = {
                Text(text = emoji, fontSize = 24.sp)
            },
            trail = {
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_editor_check_purple),
                        contentDescription = stringResource(R.string.finance_editor_selected),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onClick),
            height = 56.dp,
            rowHorizontalPadding = 20.dp,
            contentHorizontalPadding = 12.dp,
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (!isLast) HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorDateSheet(
    date: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text(stringResource(R.string.finance_editor_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.finance_editor_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTimeSheet(
    time: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var hours by remember(time) { mutableStateOf(time.hour.toString().padStart(2, '0')) }
    var minutes by remember(time) { mutableStateOf(time.minute.toString().padStart(2, '0')) }
    val hoursRequester = remember { FocusRequester() }
    val minutesRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { hoursRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { EditorSheetHandle() }) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Text(
                stringResource(R.string.finance_editor_enter_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                TimePartField(
                    value = hours,
                    onValueChange = {
                        hours = it.filter(Char::isDigit).take(2)
                        if (hours.length == 2) minutesRequester.requestFocus()
                    },
                    focusRequester = hoursRequester,
                    label = stringResource(R.string.finance_editor_hours),
                    modifier = Modifier.weight(1f).alignBy(FirstBaseline),
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alignBy(FirstBaseline),
                )
                TimePartField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                    focusRequester = minutesRequester,
                    label = stringResource(R.string.finance_editor_minutes),
                    modifier = Modifier.weight(1f).alignBy(FirstBaseline),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_editor_time),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.finance_editor_cancel)) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    val newTime =
                        LocalTime.of(
                            hours.toIntOrNull()?.takeIf { it in 0..23 } ?: time.hour,
                            minutes.toIntOrNull()?.takeIf { it in 0..59 } ?: time.minute,
                        )
                    onConfirm(newTime)
                }) { Text(stringResource(R.string.finance_editor_apply)) }
            }
        }
    }
}

@Composable
private fun TimePartField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester?,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .then(if (focusRequester == null) Modifier else Modifier.focusRequester(focusRequester))
                    .wrapContentHeight()
                    .then(
                        if (focusRequester == null) {
                            Modifier
                        } else {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp),
                            )
                        },
                    ).background(
                        if (focusRequester ==
                            null
                        ) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    ).padding(vertical = 10.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle =
                MaterialTheme.typography.displayMedium.copy(
                    textAlign = TextAlign.Center,
                    color =
                        if (focusRequester == null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                ),
        )
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 7.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTextSheet(
    title: String,
    value: String,
    singleLine: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { EditorSheetHandle() }) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            EditorSheetTitle(title)
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(
                            focusRequester,
                        ).heightIn(min = if (singleLine) 56.dp else 144.dp),
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 2,
            )
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                EditorConfirmFab(enabled = true, isSaving = false) { onConfirm(draft) }
            }
        }
    }
}

@Composable
internal fun EditorSheetTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
internal fun rememberEditorDateFormatter(): DateTimeFormatter {
    val locale =
        LocalConfiguration.current.locales[0]?.let { Locale.forLanguageTag(it.toLanguageTag()) }
            ?: LocalLocale.current.platformLocale
    return remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
}

internal val editorTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private enum class TransactionNestedSheet { Category, Date, Time, Account, Comment }
