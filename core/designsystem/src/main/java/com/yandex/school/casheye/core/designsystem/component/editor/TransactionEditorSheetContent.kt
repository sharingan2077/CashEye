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
internal fun EditorOptionContent(
    title: String,
    options: List<EditorOption>,
    selectedId: Int?,
    onSelect: (EditorOption) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    EditorOptionSelectionContent(
        title = title,
        options = options,
        selectedId = selectedId,
        query = query,
        onQueryChange = { query = it },
        onSelect = onSelect,
    )
    Spacer(Modifier.height(20.dp))
}

@Composable
internal fun EditorAccountContent(
    accounts: List<EditorOption>,
    selectedId: Int?,
    onSelect: (EditorOption) -> Unit,
) {
    val listState = rememberLazyListState()
    val gestureCoordinator = rememberSheetListGestureCoordinator(listState)
    Column(Modifier.fillMaxWidth()) {
        EditorSheetTitle(stringResource(R.string.finance_editor_accounts))
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .nestedScroll(gestureCoordinator),
            state = listState,
            flingBehavior = gestureCoordinator,
            overscrollEffect = null,
        ) {
            itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                EditorSelectionRow(
                    emoji = account.emoji,
                    title = account.label,
                    subtitle = account.subtitle,
                    selected = account.id == selectedId,
                    isLast = index == accounts.lastIndex,
                    onClick = { onSelect(account) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorDateDialog(
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
internal fun EditorTimeContent(
    time: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var isAnalogMode by remember { mutableStateOf(false) }
    val pickerState =
        rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.finance_editor_enter_time),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
        )
        if (isAnalogMode) {
            TimePicker(
                state = pickerState,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TimeInput(
                state = pickerState,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 24.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = { isAnalogMode = !isAnalogMode },
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (isAnalogMode) {
                                R.drawable.ic_editor_keyboard
                            } else {
                                R.drawable.ic_editor_time
                            },
                        ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription =
                        stringResource(
                            if (isAnalogMode) {
                                R.string.finance_editor_switch_to_time_input
                            } else {
                                R.string.finance_editor_switch_to_time_picker
                            },
                        ),
                    modifier = Modifier.size(24.dp),
                )
            }
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.finance_editor_cancel))
                }
                TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                    Text(stringResource(R.string.finance_editor_apply))
                }
            }
        }
    }
}


