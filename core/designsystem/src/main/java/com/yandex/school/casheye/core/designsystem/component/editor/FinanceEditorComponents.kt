package com.yandex.school.casheye.core.designsystem.component.editor

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.R
import com.yandex.school.casheye.core.designsystem.component.IconCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.component.ListItemDefaults
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import kotlinx.coroutines.android.awaitFrame

@Composable
fun EditorRow(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier) {
        ListItem(
            modifier =
                Modifier.then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
            minHeight = ListItemDefaults.MediumMinHeight,
            leadingContent = {
                IconCircle(
                    iconPainter = painterResource(icon),
                    contentDescription = null,
                    containerSize = 40.dp,
                    iconSize = 18.dp,
                    iconTint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = { ValuePill(value = value) },
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
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
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    CircleShape,
                ).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
internal fun EditorConfirmFab(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        containerColor =
            if (enabled) {
                MaterialTheme.colorScheme.inverseSurface
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        contentColor =
            if (enabled) {
                MaterialTheme.colorScheme.inverseOnSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
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

@Preview(
    name = "Editor Confirm Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Editor Confirm Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EditorConfirmFabPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EditorConfirmFab(enabled = true, isSaving = false, onClick = {})
                EditorConfirmFab(enabled = false, isSaving = false, onClick = {})
            }
        }
    }
}

@Composable
fun EditorSelectionRow(
    emoji: String,
    title: String,
    subtitle: String?,
    selected: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ListItem(
            leadingContent = { Text(text = emoji, fontSize = 24.sp) },
            trailingContent = {
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_editor_check_purple),
                        contentDescription = stringResource(R.string.finance_editor_selected),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onClick),
            minHeight =
                if (subtitle == null) {
                    ListItemDefaults.CompactMinHeight
                } else {
                    ListItemDefaults.DefaultMinHeight
                },
            contentPadding = ListItemDefaults.InsetContentPadding,
            slotSpacing = ListItemDefaults.DenseSlotSpacing,
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (!isLast) HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
    }
}

@Composable
fun EditorTextContent(
    title: String,
    value: String,
    placeholder: String,
    singleLine: Boolean,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    var draft by remember(value) { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
    }
    Column(modifier.fillMaxWidth().imePadding()) {
        EditorSheetTitle(title)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text(placeholder) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .heightIn(min = if (singleLine) 56.dp else 128.dp)
                    .padding(horizontal = 20.dp),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 4,
            maxLines = if (singleLine) 1 else 4,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp, end = 24.dp),
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
fun EditorSheetTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}
