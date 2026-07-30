package com.yandex.school.casheye.app.navigation.chrome

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.R
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.model.DatePeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationTopBar(
    period: DatePeriod,
    modifier: Modifier = Modifier,
    onDateClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0] ?: LocalLocale.current.platformLocale
    val dateInteractionSource = remember { MutableInteractionSource() }

    TopAppBar(
        modifier = modifier,
        title = {
            NavigationTopBarTitle(
                period = period,
                locale = locale,
                interactionSource = dateInteractionSource,
                onClick = onDateClick,
            )
        },
        actions = {
            NavigationTopBarActions(
                onAnalyticsClick = onAnalyticsClick,
                onSettingsClick = onSettingsClick,
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

@Composable
private fun NavigationTopBarTitle(
    period: DatePeriod,
    locale: java.util.Locale,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .offset(x = (-4).dp)
                .height(48.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .indication(interactionSource, ripple()),
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
                    text = period.formatted(locale),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun DatePeriod.formatted(locale: java.util.Locale): String =
    when {
        startDate == endDate -> startDate.format(DateTimeFormatter.ofPattern("d MMMM", locale))
        startDate.year == endDate.year -> {
            val formatter = DateTimeFormatter.ofPattern("d MMMM", locale)
            "${startDate.format(formatter)} – ${endDate.format(formatter)}"
        }

        else -> {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", locale)
            "${startDate.format(formatter)} – ${endDate.format(formatter)}"
        }
    }

@Composable
private fun NavigationTopBarActions(
    onAnalyticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onAnalyticsClick) {
            Icon(
                painter = painterResource(R.drawable.analytics),
                contentDescription = stringResource(R.string.content_description_analytics),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                painter = painterResource(R.drawable.sliders_horizontal),
                contentDescription = stringResource(R.string.content_description_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun NavigationTopBarPreview() {
    CashEyeTheme(dynamicColor = false) {
        NavigationTopBar(
            period = DatePeriod(LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 12)),
            onDateClick = {},
            onAnalyticsClick = {},
            onSettingsClick = {},
        )
    }
}
