package com.yandex.school.casheye.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.R
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationTopBar(
    date: LocalDate,
    onDateClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern(
            "d MMMM",
            Locale.forLanguageTag("ru"),
        )
    }
    val dateInteractionSource = remember { MutableInteractionSource() }

    TopAppBar(
        title = {
            Box(
                modifier = Modifier
                    .offset(x = (-4).dp)
                    .height(48.dp)
                    .clickable(
                        interactionSource = dateInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onDateClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .indication(dateInteractionSource, ripple()),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.calendar_month),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = date.format(dateFormatter),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onAnalyticsClick) {
                    Icon(
                        painter = painterResource(R.drawable.analytics),
                        contentDescription = "Аналитика",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onFilterClick) {
                    Icon(
                        painter = painterResource(R.drawable.sliders_horizontal),
                        contentDescription = "Фильтры",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun NavigationTopBarPreview() {
    CashEyeTheme(dynamicColor = false) {
        NavigationTopBar(
            date = LocalDate.of(2026, 6, 12),
            onDateClick = {},
            onAnalyticsClick = {},
            onFilterClick = {},
        )
    }
}
