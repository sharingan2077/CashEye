package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun MoneyListItem(
    emoji: String,
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        lead = {
            EmojiCircle(emoji = emoji)
        },
        trail = {
            Text(
                text = amount,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
            )
        },
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
