package com.yandex.school.casheye.feature.settings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.feature.settings.R
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun SettingsSheetRoute(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentDestination by rememberUpdatedState(state.destination)

    LaunchedEffect(viewModel) {
        viewModel.onIntent(SettingsIntent.Reset)
    }

    SettingsSheet(
        state = state,
        onIntent = viewModel::onIntent,
        onDismiss = onDismiss,
        currentDestination = currentDestination,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onDismiss: () -> Unit,
    currentDestination: SettingsDestination,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { target ->
                if (target == SheetValue.Hidden && currentDestination != SettingsDestination.Root) {
                    onIntent(SettingsIntent.BackToRoot)
                    false
                } else {
                    true
                }
            },
        )
    ModalBottomSheet(
        onDismissRequest = {
            if (state.destination == SettingsDestination.Root) onDismiss() else onIntent(SettingsIntent.BackToRoot)
        },
        sheetState = sheetState,
        dragHandle = { SettingsSheetHandle() },
    ) {
        BackHandler(enabled = state.destination != SettingsDestination.Root) {
            onIntent(SettingsIntent.BackToRoot)
        }
        when (state.destination) {
            SettingsDestination.Root -> SettingsRootContent(onIntent)
            else -> SettingsPlaceholderContent(destination = state.destination)
        }
    }
}

@Composable
private fun SettingsSheetHandle() {
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

@Composable
private fun ColumnScope.SettingsRootContent(
    onIntent: (SettingsIntent) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
    SettingsGroup(title = stringResource(R.string.settings_wallet)) {
        SettingsRow(
            symbol = "₽",
            label = stringResource(R.string.settings_currency),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Currency)) },
        )
        SettingsRow(
            symbol = "≡",
            label = stringResource(R.string.settings_articles),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Articles)) },
        )
    }
    SettingsGroup(title = stringResource(R.string.settings_interface)) {
        SettingsRow(
            symbol = "◐",
            label = stringResource(R.string.settings_appearance),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Appearance)) },
        )
        SettingsRow(
            symbol = "◎",
            label = stringResource(R.string.settings_language),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Language)) },
        )
    }
    SettingsGroup(title = stringResource(R.string.settings_security)) {
        SettingsRow(
            symbol = "•",
            label = stringResource(R.string.settings_pin),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Pin)) },
        )
        SettingsRow(
            symbol = "⌁",
            label = stringResource(R.string.settings_biometrics),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Biometrics)) },
        )
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
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        content()
    }
}

@Composable
private fun SettingsRow(
    symbol: String,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColumnScope.SettingsPlaceholderContent(
    destination: SettingsDestination,
) {
    val title =
        when (destination) {
            SettingsDestination.Currency -> stringResource(R.string.settings_currency)
            SettingsDestination.Articles -> stringResource(R.string.settings_articles)
            SettingsDestination.Appearance -> stringResource(R.string.settings_appearance)
            SettingsDestination.Language -> stringResource(R.string.settings_language)
            SettingsDestination.Pin -> stringResource(R.string.settings_pin)
            SettingsDestination.Biometrics -> stringResource(R.string.settings_biometrics)
            SettingsDestination.Root -> error("Root destination is rendered separately")
        }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
    )
    HorizontalDivider()
    Text(
        text = stringResource(R.string.settings_available_in_next_plan),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(20.dp),
    )
    Spacer(Modifier.height(20.dp))
}
