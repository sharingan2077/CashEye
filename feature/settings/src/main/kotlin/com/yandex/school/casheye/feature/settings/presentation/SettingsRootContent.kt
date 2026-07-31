package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.IconCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.component.ListItemDefaults
import com.yandex.school.casheye.feature.settings.R

@Composable
internal fun ColumnScope.SettingsRootContent(onIntent: (SettingsIntent) -> Unit) {
    Text(
        stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
    SettingsGroup(stringResource(R.string.settings_wallet)) {
        SettingsRow(painterResource(R.drawable.ic_settings_currency), stringResource(R.string.settings_currency)) {
            onIntent(SettingsIntent.OpenDestination(SettingsDestination.Currency))
        }
        SettingsRow(painterResource(R.drawable.ic_settings_category), stringResource(R.string.settings_articles)) {
            onIntent(SettingsIntent.OpenDestination(SettingsDestination.Articles))
        }
    }
    SettingsGroup(stringResource(R.string.settings_interface)) {
        SettingsRow(painterResource(R.drawable.ic_settings_theme), stringResource(R.string.settings_appearance)) {
            onIntent(SettingsIntent.OpenDestination(SettingsDestination.Appearance))
        }
        SettingsRow(painterResource(R.drawable.icon_settings_language), stringResource(R.string.settings_language)) {
            onIntent(SettingsIntent.OpenDestination(SettingsDestination.Language))
        }
    }
    SettingsGroup(stringResource(R.string.settings_security)) {
        SettingsRow(painterResource(R.drawable.icon_settings_pin), stringResource(R.string.settings_pin)) {
            onIntent(SettingsIntent.OpenDestination(SettingsDestination.Pin))
        }
        SettingsRow(
            painterResource(R.drawable.icon_settings_biometrics),
            stringResource(R.string.settings_biometrics),
        ) {
            onIntent(SettingsIntent.OpenDestination(SettingsDestination.Biometrics))
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        content()
    }
}

@Composable
private fun SettingsRow(
    painter: Painter,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    ListItem(
        contentPadding = ListItemDefaults.InsetContentPadding,
        minHeight = ListItemDefaults.MediumMinHeight,
        modifier =
            Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        leadingContent = {
            IconCircle(
                iconPainter = painter,
                contentDescription = null,
                iconSize = 20.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                iconTint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) { Text(label, style = MaterialTheme.typography.bodyLarge) }
}
