package com.yandex.school.casheye.core.designsystem.component.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme

@Composable
fun NativeMoneySummary(
    title: String,
    nativeTotals: List<String>,
    modifier: Modifier = Modifier,
    valuation: String? = null,
    warning: String? = null,
) {
    val displayedTotals = nativeTotals.ifEmpty { listOf("—") }
    val typography = MaterialTheme.typography
    val textMeasurer = rememberTextMeasurer()
    val summaryTextStyles =
        listOf(
            typography.displayMedium,
            typography.displaySmall,
            typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            typography.headlineMedium,
            typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
    val preferredStyleIndex =
        when (nativeTotals.size) {
            0 -> 2
            1 -> 0
            2 -> 1
            3 -> 2
            4 -> 3
            else -> 4
        }

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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = constraints.maxWidth
            val summaryTextStyle =
                remember(displayedTotals, availableWidth, typography, textMeasurer) {
                    summaryTextStyles
                        .drop(preferredStyleIndex)
                        .firstOrNull { style ->
                            displayedTotals.all { total ->
                                textMeasurer
                                    .measure(
                                        text = AnnotatedString(total),
                                        style = style,
                                        maxLines = 1,
                                        softWrap = false,
                                    ).size.width <= availableWidth
                            }
                        } ?: summaryTextStyles.last()
                }

            Column(modifier = Modifier.fillMaxWidth()) {
                displayedTotals.forEach { total ->
                    Text(
                        text = total,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = summaryTextStyle,
                    )
                }
            }
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
