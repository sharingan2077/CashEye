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

            TransactionNestedSheet.Time -> {
                EditorTimeContent(
                    time = time,
                    onConfirm = {
                        onTimeChange(it)
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
                    onAmountFocusRequest = { shouldRequestAmountFocus = false },
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
            this == TransactionNestedSheet.Time ||
            this == TransactionNestedSheet.Account ||
            this == TransactionNestedSheet.Comment
