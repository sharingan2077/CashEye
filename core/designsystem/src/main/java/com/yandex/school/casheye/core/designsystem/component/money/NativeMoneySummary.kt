package com.yandex.school.casheye.core.designsystem.component.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme

@Composable
fun NativeMoneySummary(
    title: String,
    total: String?,
    nativeTotals: List<String>,
    modifier: Modifier = Modifier,
    valuation: String? = null,
    warning: String? = null,
) {
    val displayedTotals = nativeTotals.ifEmpty { listOf("—") }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 117.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = total ?: "—",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.displaySmall,
        )
        displayedTotals.forEach { nativeTotal ->
            Text(
                text = nativeTotal,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        valuation?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        warning?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(name = "One currency", showBackground = true, widthDp = 360)
@Composable
private fun NativeMoneySummaryOneCurrencyPreview() {
    CashEyeTheme {
        NativeMoneySummary(
            title = "income, total",
            total = "7 115 411,99 €",
            nativeTotals = listOf("7 115 411,99 €"),
        )
    }
}

@Preview(name = "Two currencies", showBackground = true, widthDp = 360)
@Composable
private fun NativeMoneySummaryTwoCurrenciesPreview() {
    CashEyeTheme {
        NativeMoneySummary(
            title = "income, total",
            total = "7 482 400,99 €",
            nativeTotals =
                listOf(
                    "7 115 411,99 €",
                    "366 988 888 CN¥",
                ),
        )
    }
}

@Preview(name = "Five currencies", showBackground = true, widthDp = 360)
@Composable
private fun NativeMoneySummaryFiveCurrenciesPreview() {
    CashEyeTheme {
        NativeMoneySummary(
            title = "balance, total",
            total = "7 482 400,99 €",
            nativeTotals =
                listOf(
                    "7 115 411,99 €",
                    "376 032 ₽",
                    "16 547 $",
                    "3 333 £",
                    "367 039 722 CN¥",
                ),
        )
    }
}
