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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.core.designsystem.component.editor.CurrencySelectionContent
import com.yandex.school.casheye.core.designsystem.component.editor.EditorSelectionRow
import com.yandex.school.casheye.core.designsystem.component.editor.EditorSheetTitle
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.domain.settings.AppLanguage
import com.yandex.school.casheye.domain.settings.ThemeMode
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
private fun ColumnScope.PinContent(
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
    Text(
        text = stringResource(R.string.settings_pin_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { index ->
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (index < value.length) "•" else "")
                }
            }
        }
        androidx.compose.foundation.text.BasicTextField(
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
                    .focusRequester(focusRequester),
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
private fun ColumnScope.BiometricsContent(
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
        )
    }
    Spacer(Modifier.height(20.dp))
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
private fun ColumnScope.SettingsRootContent(onIntent: (SettingsIntent) -> Unit) {
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
private fun ColumnScope.CurrencyContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    CurrencySelectionContent(
        title = stringResource(R.string.settings_currency),
        selectedCurrency = state.reportingCurrency.isoCode,
        onSelect = { onIntent(SettingsIntent.SelectReportingCurrency(it.toCurrencyCode())) },
    )
}

@Composable
private fun ColumnScope.ArticlesContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    EditorSheetTitle(stringResource(R.string.settings_articles))
    OutlinedTextField(
        value = state.articlesQuery,
        onValueChange = { onIntent(SettingsIntent.ArticlesQueryChanged(it)) },
        placeholder = { Text(stringResource(R.string.settings_articles_search)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    )
    when {
        state.isArticlesLoading && state.articles.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        state.articlesError != null && state.articles.isEmpty() -> {
            Text(
                text = stringResource(R.string.settings_articles_load_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onIntent(SettingsIntent.LoadArticles) }
                        .padding(20.dp),
            )
        }

        state.visibleArticles.isEmpty() -> {
            Text(
                text = stringResource(R.string.settings_articles_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }

        else -> {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                itemsIndexed(state.visibleArticles, key = { _, category -> category.id }) { index, category ->
                    EditorSelectionRow(
                        emoji = category.emoji,
                        title = category.name,
                        subtitle = null,
                        selected = false,
                        isLast = index == state.visibleArticles.lastIndex,
                        onClick = {},
                    )
                }
            }
            if (state.articlesError != null) {
                Text(
                    text = stringResource(R.string.settings_articles_load_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(SettingsIntent.LoadArticles) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun ColumnScope.AppearanceContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    EditorSheetTitle(stringResource(R.string.settings_appearance))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeOption(
            label = stringResource(R.string.settings_theme_light),
            symbol = "☼",
            mode = ThemeMode.LIGHT,
            selected = state.settings.themeMode == ThemeMode.LIGHT,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(SettingsIntent.SelectThemeMode(ThemeMode.LIGHT)) },
        )
        ThemeOption(
            label = stringResource(R.string.settings_theme_dark),
            symbol = "☾",
            mode = ThemeMode.DARK,
            selected = state.settings.themeMode == ThemeMode.DARK,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(SettingsIntent.SelectThemeMode(ThemeMode.DARK)) },
        )
        ThemeOption(
            label = stringResource(R.string.settings_theme_system),
            symbol = "▣",
            mode = ThemeMode.SYSTEM,
            selected = state.settings.themeMode == ThemeMode.SYSTEM,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(SettingsIntent.SelectThemeMode(ThemeMode.SYSTEM)) },
        )
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun ThemeOption(
    label: String,
    symbol: String,
    mode: ThemeMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val previewColor =
        when (mode) {
            ThemeMode.LIGHT -> MaterialTheme.colorScheme.surface
            ThemeMode.DARK -> MaterialTheme.colorScheme.inverseSurface
            ThemeMode.SYSTEM -> MaterialTheme.colorScheme.surfaceVariant
        }
    Column(
        modifier =
            modifier
                .border(1.dp, borderColor, MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(previewColor),
        )
        Text(
            text = "$symbol $label",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) borderColor else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ColumnScope.LanguageContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    EditorSheetTitle(stringResource(R.string.settings_language))
    LanguageOption(
        flag = "🇷🇺",
        label = stringResource(R.string.settings_language_russian),
        selected = state.settings.language == AppLanguage.RUSSIAN,
        onClick = { onIntent(SettingsIntent.SelectLanguage(AppLanguage.RUSSIAN)) },
    )
    LanguageOption(
        flag = "🇬🇧",
        label = stringResource(R.string.settings_language_english),
        selected = state.settings.language == AppLanguage.ENGLISH,
        onClick = { onIntent(SettingsIntent.SelectLanguage(AppLanguage.ENGLISH)) },
    )
    LanguageOption(
        flag = "🇩🇪",
        label = stringResource(R.string.settings_language_german),
        selected = state.settings.language == AppLanguage.GERMAN,
        onClick = { onIntent(SettingsIntent.SelectLanguage(AppLanguage.GERMAN)) },
    )
    LanguageOption(
        flag = "🇫🇷",
        label = stringResource(R.string.settings_language_french),
        selected = state.settings.language == AppLanguage.FRENCH,
        onClick = { onIntent(SettingsIntent.SelectLanguage(AppLanguage.FRENCH)) },
    )
    LanguageOption(
        flag = "🇪🇸",
        label = stringResource(R.string.settings_language_spanish),
        selected = state.settings.language == AppLanguage.SPANISH,
        isLast = true,
        onClick = { onIntent(SettingsIntent.SelectLanguage(AppLanguage.SPANISH)) },
    )
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun LanguageOption(
    flag: String,
    label: String,
    selected: Boolean,
    isLast: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = flag, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (!isLast) HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
}

private fun String.toCurrencyCode() = CurrencyCode.fromIsoCode(this)

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
