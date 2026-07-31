package com.yandex.school.casheye.feature.settings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.feature.settings.R
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun SettingsSheetRoute(
    onDismiss: () -> Unit,
    biometricsAvailable: Boolean,
    onRequestBiometricEnable: (onResult: (Boolean) -> Unit) -> Unit,
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
        biometricsAvailable = biometricsAvailable,
        onRequestBiometricEnable = onRequestBiometricEnable,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onDismiss: () -> Unit,
    currentDestination: SettingsDestination,
    biometricsAvailable: Boolean,
    onRequestBiometricEnable: (onResult: (Boolean) -> Unit) -> Unit,
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        BackHandler(enabled = state.destination != SettingsDestination.Root) {
            onIntent(SettingsIntent.BackToRoot)
        }
        when (state.destination) {
            SettingsDestination.Root -> {
                SettingsRootContent(onIntent)
            }

            SettingsDestination.Currency -> {
                CurrencyContent(state, onIntent)
            }

            SettingsDestination.Articles -> {
                ArticlesContent(state, onIntent)
            }

            SettingsDestination.Appearance -> {
                AppearanceContent(state, onIntent)
            }

            SettingsDestination.Language -> {
                LanguageContent(state, onIntent)
            }

            SettingsDestination.Pin -> {
                PinContent(state, onIntent)
            }

            SettingsDestination.Biometrics -> {
                BiometricsContent(
                    state = state,
                    biometricsAvailable = biometricsAvailable,
                    onIntent = onIntent,
                    onRequestBiometricEnable = onRequestBiometricEnable,
                )
            }

            else -> {
                SettingsPlaceholderContent(destination = state.destination)
            }
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
private fun ColumnScope.SettingsPlaceholderContent(destination: SettingsDestination) {
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
