package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun IconFilterCircle(
    iconPainter: Painter,
    contentDescription: String?
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(
                width = 0.75f.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = contentDescription
        )
    }
}
