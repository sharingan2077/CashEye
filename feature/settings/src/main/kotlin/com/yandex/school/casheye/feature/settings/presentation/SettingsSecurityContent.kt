package com.yandex.school.casheye.feature.settings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.core.designsystem.component.IconCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.component.ListItemDefaults
import com.yandex.school.casheye.core.designsystem.component.editor.CurrencySelectionContent
import com.yandex.school.casheye.core.designsystem.component.editor.EditorOption
import com.yandex.school.casheye.core.designsystem.component.editor.EditorOptionSelectionContent
import com.yandex.school.casheye.core.designsystem.component.editor.EditorSheetTitle
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.domain.settings.AppLanguage
import com.yandex.school.casheye.domain.settings.ThemeMode
import com.yandex.school.casheye.feature.settings.R
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
internal fun ColumnScope.PinContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var value by remember { mutableStateOf("") }
    val isConfigured = state.settings.security.pinVerifier != null
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                Box(
                    modifier =
                        if (index < value.length) {
                            Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                                .size(16.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        }.testTag("settings_pin_cell_$index"),
                    contentAlignment = Alignment.Center,
                ) {}
            }
        }
        BasicTextField(
            value = value,
            onValueChange = { input ->
                val digits = input.filter(Char::isDigit).take(4)
                value = digits
                if (digits.length == 4) {
                    onIntent(SettingsIntent.SetPin(digits.toCharArray()))
                    value = ""
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(0f)
                    .focusRequester(focusRequester)
                    .testTag("settings_pin_input"),
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
