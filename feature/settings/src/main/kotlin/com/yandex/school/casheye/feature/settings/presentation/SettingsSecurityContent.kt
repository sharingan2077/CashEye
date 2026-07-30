package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.editor.EditorSheetTitle
import com.yandex.school.casheye.feature.settings.R

@Composable
internal fun ColumnScope.PinContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val isConfigured = state.settings.security.pinVerifier != null

    EditorSheetTitle(
        stringResource(if (isConfigured) R.string.settings_pin_change else R.string.settings_pin_set),
    )
    Column(
        modifier =
            Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_pin_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .align(
                        Alignment.CenterHorizontally,
                    ).padding(top = 24.dp),
        )
        PinCodeInput(
            value = value,
            inputTestTag = "settings_pin_input",
            cellTestTagPrefix = "settings_pin_cell",
            onValueChange = { value = it },
            onPinComplete = { pin ->
                onIntent(SettingsIntent.SetPin(pin.toCharArray()))
                value = ""
            },
        )
    }
    if (isConfigured) {
        Text(
            text = stringResource(R.string.settings_pin_disable),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .padding(20.dp)
                    .clickable { onIntent(SettingsIntent.DisablePin) },
        )
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
internal fun ColumnScope.BiometricsContent(
    state: SettingsUiState,
    biometricsAvailable: Boolean,
    onIntent: (SettingsIntent) -> Unit,
    onRequestBiometricEnable: (onResult: (Boolean) -> Unit) -> Unit,
) {
    val hasPin = state.settings.security.pinVerifier != null
    val enabled = state.settings.security.biometricsEnabled
    EditorSheetTitle(stringResource(R.string.settings_biometrics))
    Text(
        text =
            stringResource(
                when {
                    !hasPin -> R.string.settings_biometrics_pin_required
                    !biometricsAvailable -> R.string.settings_biometrics_unavailable
                    else -> R.string.settings_biometrics_hint
                },
            ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(20.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.settings_biometrics_enable), modifier = Modifier.weight(1f))
        Switch(
            checked = enabled,
            enabled = hasPin && biometricsAvailable,
            onCheckedChange = { checked ->
                if (checked) {
                    onRequestBiometricEnable { success ->
                        if (success) onIntent(SettingsIntent.SetBiometricsEnabled(true))
                    }
                } else {
                    onIntent(SettingsIntent.SetBiometricsEnabled(false))
                }
            },
            modifier = Modifier.testTag("settings_biometrics_toggle"),
        )
    }
    Spacer(Modifier.height(20.dp))
}
