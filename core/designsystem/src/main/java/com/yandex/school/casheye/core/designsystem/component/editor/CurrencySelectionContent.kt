package com.yandex.school.casheye.core.designsystem.component.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.R

@Composable
fun CurrencySelectionContent(
    title: String,
    selectedCurrency: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        EditorSheetTitle(title)
        currencyOptions.forEachIndexed { index, currency ->
            EditorSelectionRow(
                emoji = currency.flag,
                title = stringResource(currency.titleRes),
                subtitle = currency.code,
                selected = currency.code == selectedCurrency,
                isLast = index == currencyOptions.lastIndex,
                onClick = { onSelect(currency.code) },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

private data class CurrencyOption(
    val code: String,
    val flag: String,
    @StringRes
    val titleRes: Int,
)

private val currencyOptions =
    listOf(
        CurrencyOption("RUB", "🇷🇺", R.string.finance_editor_currency_rub),
        CurrencyOption("USD", "🇺🇸", R.string.finance_editor_currency_usd),
        CurrencyOption("EUR", "🇪🇺", R.string.finance_editor_currency_eur),
        CurrencyOption("GBP", "🇬🇧", R.string.finance_editor_currency_gbp),
        CurrencyOption("CNY", "🇨🇳", R.string.finance_editor_currency_cny),
    )
