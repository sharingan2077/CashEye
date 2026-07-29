package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    minHeight: Dp = ListItemDefaults.DefaultMinHeight,
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
    slotSpacing: Dp = ListItemDefaults.SlotSpacing,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(slotSpacing))
        }
        Box(
            modifier =
                Modifier
                    .weight(1f),
        ) {
            content()
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(slotSpacing))
            trailingContent()
        }
    }
}
