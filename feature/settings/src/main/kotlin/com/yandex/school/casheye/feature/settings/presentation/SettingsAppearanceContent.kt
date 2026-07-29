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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
internal fun ColumnScope.AppearanceContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    EditorSheetTitle(stringResource(R.string.settings_appearance))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThemeOption(
            label = stringResource(R.string.settings_theme_light),
            painter = painterResource(R.drawable.ic_settings_sun),
            mode = ThemeMode.LIGHT,
            selected = state.settings.themeMode == ThemeMode.LIGHT,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(SettingsIntent.SelectThemeMode(ThemeMode.LIGHT)) },
        )
        ThemeOption(
            label = stringResource(R.string.settings_theme_dark),
            painter = painterResource(R.drawable.ic_settings_moon),
            mode = ThemeMode.DARK,
            selected = state.settings.themeMode == ThemeMode.DARK,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(SettingsIntent.SelectThemeMode(ThemeMode.DARK)) },
        )
        ThemeOption(
            label = stringResource(R.string.settings_theme_system),
            painter = painterResource(R.drawable.ic_settings_monitor),
            mode = ThemeMode.SYSTEM,
            selected = state.settings.themeMode == ThemeMode.SYSTEM,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(SettingsIntent.SelectThemeMode(ThemeMode.SYSTEM)) },
        )
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
internal fun ColumnScope.LanguageContent(
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
    ListItem(
        modifier =
            Modifier
                .clickable(role = Role.RadioButton, onClick = onClick),
        minHeight = ListItemDefaults.CompactMinHeight,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        slotSpacing = 12.dp,
        leadingContent = { Text(text = flag, style = MaterialTheme.typography.titleMedium) },
        trailingContent =
            if (selected) {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_check),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
    if (!isLast) HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
}


