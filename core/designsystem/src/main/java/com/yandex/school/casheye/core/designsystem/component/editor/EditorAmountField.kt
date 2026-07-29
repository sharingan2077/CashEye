package com.yandex.school.casheye.core.designsystem.component.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.R
import com.yandex.school.casheye.core.designsystem.component.datepicker.rememberPastOrPresentSelectableDates
import com.yandex.school.casheye.core.designsystem.component.money.currencySymbol
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun EditorAmountField(
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

    val amountInteractionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = amountInteractionSource,
                    indication = null,
                    onClick = { focusRequester.requestFocus() },
                ),
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
                        MaterialTheme.typography.displayMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                        ),
                )
                if (fieldValue.text.isNotEmpty()) {
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.displayMedium,
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
    val change = AmountFieldChange.from(previousValue, proposedValue, locale)
    val next = change.toAmountEdit(normalized)
    val resolved = change.resolveAmountEdit(normalized, next)
    val text = formatAmount(resolved.amount, locale)
    return AmountFieldUpdate(
        canonicalAmount = resolved.amount,
        fieldValue = TextFieldValue(text, TextRange(visualOffset(text, resolved.selection, locale))),
    )
}

private data class AmountEdit(
    val amount: String,
    val selection: Int,
)

private data class AmountFieldChange(
    val previousValue: TextFieldValue,
    val proposedValue: TextFieldValue,
    val locale: Locale,
    val removed: String,
    val inserted: String,
    val selectionOffset: Int,
) {
    fun toAmountEdit(normalized: String): AmountEdit =
        separatorEdit(normalized) ?: fractionReplacement(normalized) ?: fractionDeletion(normalized)
            ?: AmountEdit(
                canonicalFromVisual(proposedValue.text, locale),
                canonicalOffset(proposedValue.text, proposedValue.selection.start, locale),
            )

    fun resolveAmountEdit(
        normalized: String,
        edit: AmountEdit,
    ): AmountEdit {
        val proposedAmount = normalizeAmount(edit.amount)
        if (proposedAmount.substringBefore('.').length <= MAX_INTEGER_DIGITS) {
            return AmountEdit(proposedAmount, edit.selection)
        }
        val decimalOffset = normalized.indexOf('.')
        return if (keepsDecimalAtLimit(decimalOffset)) {
            AmountEdit("${normalized.substringBefore('.')}.99", decimalOffset)
        } else {
            AmountEdit(
                normalized,
                canonicalOffset(previousValue.text, previousValue.selection.start, locale),
            )
        }
    }

    private fun separatorEdit(normalized: String): AmountEdit? {
        if (!inserted.any(Char::isDecimalSeparator)) return null
        val decimalOffset = normalized.indexOf('.')
        return if (decimalOffset == -1) {
            val integerPart = normalized.ifEmpty { "0" }
            AmountEdit("$integerPart.00", integerPart.length + 1)
        } else {
            AmountEdit(normalized, canonicalOffset(previousValue.text, previousValue.selection.start, locale))
        }
    }

    private fun fractionReplacement(normalized: String): AmountEdit? {
        val decimalOffset = normalized.indexOf('.')
        if (!isFractionCell(decimalOffset) || inserted.length != 1 || !inserted[0].isDigit()) return null
        return AmountEdit(normalized.replaceRange(selectionOffset, selectionOffset + 1, inserted), selectionOffset + 1)
    }

    private fun fractionDeletion(normalized: String): AmountEdit? {
        val decimalOffset = normalized.indexOf('.')
        if (!isFractionCell(decimalOffset) || removed.length != 1 || !removed[0].isDigit()) return null
        val fraction =
            normalized.substring(decimalOffset + 1).removeRange(
                selectionOffset - decimalOffset - 1,
                selectionOffset - decimalOffset,
            )
        val amount = normalized.substring(0, decimalOffset + 1) + fraction.padEnd(2, '0')
        return AmountEdit(amount, selectionOffset)
    }

    private fun isFractionCell(decimalOffset: Int): Boolean =
        decimalOffset >= 0 &&
            selectionOffset > decimalOffset &&
            selectionOffset < decimalOffset + FRACTION_END_EXCLUSIVE_OFFSET

    private fun keepsDecimalAtLimit(decimalOffset: Int): Boolean =
        decimalOffset >= 0 &&
            selectionOffset == decimalOffset &&
            removed.singleOrNull() == DecimalFormatSymbols.getInstance(locale).decimalSeparator

    companion object {
        fun from(
            previousValue: TextFieldValue,
            proposedValue: TextFieldValue,
            locale: Locale,
        ): AmountFieldChange {
            val prefixLength = previousValue.text.commonPrefixWith(proposedValue.text).length
            val suffixLength =
                previousValue.text
                    .drop(prefixLength)
                    .commonSuffixWith(proposedValue.text.drop(prefixLength))
                    .length
            return AmountFieldChange(
                previousValue = previousValue,
                proposedValue = proposedValue,
                locale = locale,
                removed = previousValue.text.substring(prefixLength, previousValue.text.length - suffixLength),
                inserted = proposedValue.text.substring(prefixLength, proposedValue.text.length - suffixLength),
                selectionOffset = canonicalOffset(previousValue.text, prefixLength, locale),
            )
        }
    }
}

private const val MAX_INTEGER_DIGITS = 9
private const val FRACTION_END_EXCLUSIVE_OFFSET = 3

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
    val cleaned = value.filter { it.isEditableAmountCharacter(groupingSeparator) }
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

private fun Char.isEditableAmountCharacter(groupingSeparator: Char): Boolean =
    isDigit() || (this != groupingSeparator && isDecimalSeparator())

private fun Char.isDecimalSeparator(): Boolean = this == '.' || this == ','
