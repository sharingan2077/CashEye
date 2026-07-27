package com.yandex.school.casheye.core.designsystem.component.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorModalSheet(
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
fun FinanceEditorContent(
    amount: String,
    currency: String,
    isSaving: Boolean,
    isSaveEnabled: Boolean,
    error: String?,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    requestAmountFocus: Boolean,
    onAmountFocusRequest: () -> Unit,
    title: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    titleColor: Color = Color.Unspecified,
    rows: @Composable ColumnScope.(clearPrimaryFocus: () -> Unit) -> Unit,
) {
    val amountFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val currentOnAmountFocusRequest by rememberUpdatedState(onAmountFocusRequest)
    LaunchedEffect(requestAmountFocus) {
        if (!requestAmountFocus) return@LaunchedEffect
        awaitFrame()
        amountFocusRequester.requestFocus()
        currentOnAmountFocusRequest()
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
                style = titleStyle,
                color = titleColor,
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
private fun EditorSheetHandle() {
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
                    .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}
