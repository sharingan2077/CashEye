package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.editor.CurrencySelectionContent
import com.yandex.school.casheye.core.designsystem.component.editor.EditorOption
import com.yandex.school.casheye.core.designsystem.component.editor.EditorOptionSelectionContent
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.feature.settings.R

@Composable
internal fun ColumnScope.CurrencyContent(
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
internal fun ColumnScope.ArticlesContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    EditorOptionSelectionContent(
        title = stringResource(R.string.settings_articles),
        options =
            state.articles.map {
                EditorOption(it.id, it.name, it.emoji)
            },
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
                        stringResource(
                            R.string.settings_articles_load_error,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onIntent(SettingsIntent.LoadArticles)
                                }.padding(20.dp),
                    )
                }

                else -> {
                    Text(
                        stringResource(R.string.settings_articles_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        },
    )
    if (state.articles.isNotEmpty() &&
        state.articlesError != null
    ) {
        Text(
            stringResource(
                R.string.settings_articles_load_error,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onIntent(SettingsIntent.LoadArticles)
                    }.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
    Spacer(Modifier.height(20.dp))
}

private fun String.toCurrencyCode() = CurrencyCode.fromIsoCode(this)
