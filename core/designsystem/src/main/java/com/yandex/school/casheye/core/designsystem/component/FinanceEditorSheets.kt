package com.yandex.school.casheye.core.designsystem.component

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.R
import kotlinx.coroutines.android.awaitFrame
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
    var shouldRequestAmountFocus by remember { mutableStateOf(true) }
    val transactionIsValid = amount.isNotBlank() && category != null && account != null
    val dateFormatter = rememberEditorDateFormatter()
    val currentNested by rememberUpdatedState(nested)
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { target ->
                if (target == SheetValue.Hidden && currentNested.isSheetContent) {
                    nested = null
                    false
                } else {
                    true
                }
            },
        )
    EditorModalSheet(
        sheetState = sheetState,
        onDismiss = {
            if (nested.isSheetContent) {
                nested = null
            } else {
                onDismiss()
            }
        },
    ) {
        BackHandler(enabled = nested.isSheetContent) { nested = null }
        when (nested) {
            TransactionNestedSheet.Category -> {
                EditorOptionContent(
                    title = stringResource(R.string.finance_editor_categories),
                    options = categories,
                    selectedId = category?.id,
                    searchable = true,
                    onSelect = {
                        onCategoryChange(it)
                        nested = null
                    },
                )
            }

            TransactionNestedSheet.Account -> {
                EditorAccountContent(
                    accounts = accounts,
                    selectedId = account?.id,
                    onSelect = {
                        onAccountChange(it)
                        nested = null
                    },
                )
            }

            TransactionNestedSheet.Comment -> {
                EditorTextContent(
                    title = stringResource(R.string.finance_editor_comment),
                    value = comment,
                    placeholder = stringResource(R.string.finance_editor_comment_placeholder),
                    singleLine = false,
                    onConfirm = {
                        onCommentChange(it)
                        nested = null
                    },
                    onDismiss = { nested = null },
                )
            }

            else -> {
                FinanceEditorContent(
                    amount = amount,
                    currency = currencySymbol(account?.currencyCode ?: "RUB"),
                    isSaving = isSaving,
                    isSaveEnabled = transactionIsValid,
                    error = error,
                    onAmountChange = onAmountChange,
                    onSave = onSave,
                    requestAmountFocus = shouldRequestAmountFocus,
                    onAmountFocusRequested = { shouldRequestAmountFocus = false },
                ) { clearPrimaryFocus ->
                    EditorRow(
                        icon = R.drawable.ic_editor_category,
                        label = stringResource(R.string.finance_editor_category),
                        value = category?.label ?: stringResource(R.string.finance_editor_not_specified),
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
            }
        }
    }

    if (nested == TransactionNestedSheet.Date) {
        EditorDateDialog(
            date = date,
            onConfirm = {
                onDateChange(it)
                nested = null
            },
            onDismiss = { nested = null },
        )
    }
    if (nested == TransactionNestedSheet.Time) {
        EditorTimeDialog(
            time = time,
            onConfirm = {
                onTimeChange(it)
                nested = null
            },
            onDismiss = { nested = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorModalSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { EditorSheetHandle() },
        sheetState = sheetState,
        content = content,
    )
}

@Composable
internal fun FinanceEditorContent(
    amount: String,
    currency: String,
    isSaving: Boolean,
    isSaveEnabled: Boolean,
    error: String?,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    requestAmountFocus: Boolean,
    onAmountFocusRequested: () -> Unit,
    title: String? = null,
    rows: @Composable ColumnScope.(clearPrimaryFocus: () -> Unit) -> Unit,
) {
    val amountFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(requestAmountFocus) {
        if (!requestAmountFocus) return@LaunchedEffect
        awaitFrame()
        amountFocusRequester.requestFocus()
        onAmountFocusRequested()
    }

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
    val locale =
        LocalConfiguration.current.locales[0]?.let { Locale.forLanguageTag(it.toLanguageTag()) }
            ?: LocalLocale.current.platformLocale
    var renderedAmount by remember { mutableStateOf(amount) }
    var fieldValue by
        remember(locale) {
            mutableStateOf(amountFieldValue(amount, locale, selection = formatAmount(amount, locale).length))
        }

    LaunchedEffect(amount, locale) {
        if (amount != renderedAmount) {
            renderedAmount = amount
            fieldValue = amountFieldValue(amount, locale, selection = formatAmount(amount, locale).length)
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(min = 200.dp)
                    .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = fieldValue,
                    onValueChange = { proposedValue ->
                        val update =
                            updateAmountField(
                                canonicalAmount = renderedAmount,
                                previousValue = fieldValue,
                                proposedValue = proposedValue,
                                locale = locale,
                            )
                        renderedAmount = update.canonicalAmount
                        fieldValue = update.fieldValue
                        if (amount != update.canonicalAmount) onAmountChange(update.canonicalAmount)
                    },
                    modifier =
                        Modifier
                            .width(IntrinsicSize.Min)
                            .widthIn(min = 16.dp)
                            .padding(horizontal = 1.dp)
                            .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle =
                        MaterialTheme.typography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                        ),
                )
                if (fieldValue.text.isNotEmpty()) {
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

internal data class AmountFieldUpdate(
    val canonicalAmount: String,
    val fieldValue: TextFieldValue,
)

/** Formats the value shown in editor fields; persistence continues to use a dot-separated value. */
internal fun formatAmount(
    canonicalAmount: String,
    locale: Locale,
): String {
    val normalized = normalizeAmount(canonicalAmount)
    if (normalized.isEmpty()) return ""

    val symbols = DecimalFormatSymbols.getInstance(locale)
    val (integerPart, fractionPart) = normalized.split('.', limit = 2).let { it[0] to it.getOrNull(1) }
    val groupedInteger =
        integerPart
            .reversed()
            .chunked(3)
            .joinToString(symbols.groupingSeparator.toString())
            .reversed()
    return buildString {
        append(groupedInteger)
        fractionPart?.let {
            append(symbols.decimalSeparator)
            append(it)
        }
    }
}

internal fun updateAmountField(
    canonicalAmount: String,
    previousValue: TextFieldValue,
    proposedValue: TextFieldValue,
    locale: Locale,
): AmountFieldUpdate {
    val normalized = normalizeAmount(canonicalAmount)
    if (previousValue.text == proposedValue.text) {
        return AmountFieldUpdate(normalized, proposedValue)
    }

    val prefixLength = previousValue.text.commonPrefixWith(proposedValue.text).length
    val suffixLength =
        previousValue.text
            .drop(prefixLength)
            .commonSuffixWith(proposedValue.text.drop(prefixLength))
            .length
    val removed = previousValue.text.substring(prefixLength, previousValue.text.length - suffixLength)
    val inserted = proposedValue.text.substring(prefixLength, proposedValue.text.length - suffixLength)
    val selectionOffset = canonicalOffset(previousValue.text, prefixLength, locale)
    val decimalOffset = normalized.indexOf('.')

    val next =
        when {
            inserted.any { it == '.' || it == ',' } && decimalOffset == -1 -> {
                val integerPart = normalized.ifEmpty { "0" }
                AmountEdit("$integerPart.00", integerPart.length + 1)
            }

            inserted.any { it == '.' || it == ',' } -> {
                AmountEdit(normalized, canonicalOffset(previousValue.text, previousValue.selection.start, locale))
            }

            inserted.length == 1 && inserted[0].isDigit() &&
                decimalOffset >= 0 && selectionOffset > decimalOffset && selectionOffset < decimalOffset + 3 -> {
                val replacement = normalized.replaceRange(selectionOffset, selectionOffset + 1, inserted)
                AmountEdit(replacement, selectionOffset + 1)
            }

            removed.length == 1 && removed[0].isDigit() &&
                decimalOffset >= 0 && selectionOffset > decimalOffset && selectionOffset < decimalOffset + 3 -> {
                val fraction =
                    normalized.substring(decimalOffset + 1).removeRange(
                        selectionOffset - decimalOffset - 1,
                        selectionOffset - decimalOffset,
                    )
                AmountEdit(
                    normalized.substring(0, decimalOffset + 1) + fraction.padEnd(2, '0'),
                    selectionOffset,
                )
            }

            else -> {
                AmountEdit(
                    canonicalFromVisual(proposedValue.text, locale),
                    canonicalOffset(proposedValue.text, proposedValue.selection.start, locale),
                )
            }
        }

    val proposedAmount = normalizeAmount(next.amount)
    val isWithinLimit = proposedAmount.substringBefore('.').length <= MAX_INTEGER_DIGITS
    val keepsDecimalAtLimit =
        !isWithinLimit &&
            decimalOffset >= 0 &&
            selectionOffset == decimalOffset &&
            removed.singleOrNull() == DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val nextAmount =
        when {
            isWithinLimit -> proposedAmount
            keepsDecimalAtLimit -> "${normalized.substringBefore('.')}.99"
            else -> normalized
        }
    val nextSelection =
        when {
            isWithinLimit -> next.selection
            keepsDecimalAtLimit -> decimalOffset
            else -> canonicalOffset(previousValue.text, previousValue.selection.start, locale)
        }
    val text = formatAmount(nextAmount, locale)
    return AmountFieldUpdate(
        canonicalAmount = nextAmount,
        fieldValue = TextFieldValue(text, TextRange(visualOffset(text, nextSelection, locale))),
    )
}

private data class AmountEdit(
    val amount: String,
    val selection: Int,
)

private const val MAX_INTEGER_DIGITS = 9

private fun amountFieldValue(
    amount: String,
    locale: Locale,
    selection: Int,
): TextFieldValue {
    val text = formatAmount(amount, locale)
    return TextFieldValue(text, TextRange(selection.coerceIn(0, text.length)))
}

private fun normalizeAmount(value: String): String {
    val separator = value.indexOfFirst { it == '.' || it == ',' }
    val integer = (if (separator == -1) value else value.substring(0, separator)).filter(Char::isDigit)
    val normalizedInteger = integer.trimStart('0').ifEmpty { if (integer.isNotEmpty()) "0" else "" }
    if (separator == -1) return normalizedInteger
    val fraction =
        value
            .substring(separator + 1)
            .filter(Char::isDigit)
            .take(2)
            .padEnd(2, '0')
    return "${normalizedInteger.ifEmpty { "0" }}.$fraction"
}

private fun canonicalFromVisual(
    value: String,
    locale: Locale,
): String {
    val groupingSeparator = DecimalFormatSymbols.getInstance(locale).groupingSeparator
    val cleaned = value.filter { it.isDigit() || (it != groupingSeparator && (it == '.' || it == ',')) }
    return normalizeAmount(cleaned)
}

private fun canonicalOffset(
    value: String,
    visualOffset: Int,
    locale: Locale,
): Int {
    val groupingSeparator = DecimalFormatSymbols.getInstance(locale).groupingSeparator
    return value.take(visualOffset).count { it.isDigit() || (it != groupingSeparator && (it == '.' || it == ',')) }
}

private fun visualOffset(
    value: String,
    canonicalOffset: Int,
    locale: Locale,
): Int {
    val groupingSeparator = DecimalFormatSymbols.getInstance(locale).groupingSeparator
    var canonicalIndex = 0
    value.forEachIndexed { index, character ->
        if (canonicalIndex == canonicalOffset) return index
        if (character.isDigit() || (character != groupingSeparator && (character == '.' || character == ','))) {
            canonicalIndex++
        }
    }
    return value.length
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
                modifier = Modifier.padding(start = 16.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            ValuePill(
                value = value,
                modifier = Modifier.padding(start = 12.dp),
//                modifier = Modifier.weight(0.45f, fill = false),
            )
        }
        if (showDivider) HorizontalDivider()
    }
}

@Composable
private fun ValuePill(
    value: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            modifier
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

@Composable
private fun EditorOptionContent(
    title: String,
    options: List<EditorOption>,
    selectedId: Int?,
    searchable: Boolean,
    onSelect: (EditorOption) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visibleOptions = remember(options, query) { options.filter { it.label.contains(query, ignoreCase = true) } }
    Column(modifier = Modifier.fillMaxWidth()) {
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
private fun EditorDateDialog(
    date: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = rememberPastOrPresentSelectableDates(),
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
private fun EditorTimeDialog(
    time: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finance_editor_enter_time)) },
        text = { TimeInput(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.finance_editor_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.finance_editor_cancel))
            }
        },
    )
}

@Composable
internal fun EditorTextContent(
    title: String,
    value: String,
    placeholder: String,
    singleLine: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
    }
    Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        EditorSheetTitle(title)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text(placeholder) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .heightIn(min = if (singleLine) 56.dp else 128.dp),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 4,
            maxLines = if (singleLine) 1 else 4,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.finance_editor_cancel))
            }
            TextButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    onConfirm(draft)
                },
            ) {
                Text(stringResource(R.string.finance_editor_done))
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

private val TransactionNestedSheet?.isSheetContent: Boolean
    get() =
        this == TransactionNestedSheet.Category ||
            this == TransactionNestedSheet.Account ||
            this == TransactionNestedSheet.Comment
