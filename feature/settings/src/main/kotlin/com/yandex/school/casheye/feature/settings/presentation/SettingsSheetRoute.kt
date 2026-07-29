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
private fun ColumnScope.SettingsRootContent(onIntent: (SettingsIntent) -> Unit) {
    Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
    SettingsGroup(title = stringResource(R.string.settings_wallet)) {
        SettingsRow(
            painter = painterResource(R.drawable.ic_settings_currency),
            label = stringResource(R.string.settings_currency),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Currency)) },
        )
        SettingsRow(
            painter = painterResource(R.drawable.ic_settings_category),
            label = stringResource(R.string.settings_articles),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Articles)) },
        )
    }
    SettingsGroup(title = stringResource(R.string.settings_interface)) {
        SettingsRow(
            painter = painterResource(R.drawable.ic_settings_theme),
            label = stringResource(R.string.settings_appearance),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Appearance)) },
        )
        SettingsRow(
            painter = painterResource(R.drawable.icon_settings_language),
            label = stringResource(R.string.settings_language),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Language)) },
        )
    }
    SettingsGroup(title = stringResource(R.string.settings_security)) {
        SettingsRow(
            painter = painterResource(R.drawable.icon_settings_pin),
            label = stringResource(R.string.settings_pin),
            onClick = { onIntent(SettingsIntent.OpenDestination(SettingsDestination.Pin)) },
        )
        SettingsRow(
            painter = painterResource(R.drawable.icon_settings_biometrics),
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
    EditorOptionSelectionContent(
        title = stringResource(R.string.settings_articles),
        options = state.articles.map { EditorOption(it.id, it.name, it.emoji) },
        selectedId = null,
        query = state.articlesQuery,
        onQueryChange = { onIntent(SettingsIntent.ArticlesQueryChanged(it)) },
        onSelect = {},
        emptyContent = {
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

                else -> {
                    Text(
                        text = stringResource(R.string.settings_articles_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        },
    )
    if (state.articles.isNotEmpty() && state.articlesError != null) {
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
    Spacer(Modifier.height(20.dp))
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
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
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
