package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ListItem(
    lead: @Composable () -> Unit,
    trail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    rowHorizontalPadding: Dp = 16.dp,
    contentHorizontalPadding: Dp = 16.dp,
    height: Dp = 72.dp,
    content: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = rowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lead()
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = contentHorizontalPadding),
        ) {
            content()
        }
        trail()
    }
}
