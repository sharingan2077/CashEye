@file:Suppress("MagicNumber")

package com.yandex.school.casheye.feature.analytics.presentation

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.AnalyticsTransaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Preview(
    name = "Screen Light",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Screen Dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AnalyticsScreenPreview() {
    AnalyticsPreview()
}

@Composable
private fun AnalyticsPreview(activeSheet: AnalyticsSheet? = null) {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnalyticsScreen(
                state = analyticsPreviewState(activeSheet),
                onIntent = {},
            )
        }
    }
}

private fun analyticsPreviewState(activeSheet: AnalyticsSheet?): AnalyticsUiState.Content {
    val accounts =
        listOf(
            Account(1, "Основной счёт", "💳", BigDecimal("75240"), CurrencyCode.RUB),
            Account(2, "Накопительный", "🏦", BigDecimal("124000"), CurrencyCode.RUB),
        )
    val categories =
        listOf(
            Category(101, "Продукты", "🛒", false),
            Category(102, "Кафе", "☕", false),
            Category(103, "Транспорт", "🚕", false),
            Category(104, "Подписки", "📱", false),
        )
    val period =
        AnalyticsPeriod(
            startDate = LocalDate.of(2026, 7, 1),
            endDate = LocalDate.of(2026, 7, 20),
            preset = AnalyticsPeriodPreset.Month,
        )
    val data =
        AnalyticsScreenData(
            filters =
                AnalyticsFilters(
                    type = AnalyticsType.Expenses,
                    period = period,
                    accountId = accounts.first().id,
                ),
            currentDate = LocalDate.of(2026, 7, 20),
            accounts = accounts,
            categories = categories,
            activeSheet = activeSheet,
        )
    return AnalyticsUiState.Content(
        data = data,
        total = BigDecimal("18750"),
        currencyCode = CurrencyCode.RUB.isoCode,
        transactions =
            listOf(
                previewTransaction(1, accounts.first(), categories[0], "5420", "Пятёрочка"),
                previewTransaction(2, accounts.first(), categories[1], "890", "Обед"),
                previewTransaction(3, accounts.first(), categories[2], "1240", null),
            ).map { it.toAnalyticsTransaction() },
        unconvertedTransactions = emptyList(),
        categorySummaries =
            listOf(
                AnalyticsCategorySummary(categories[0], BigDecimal("10420")),
                AnalyticsCategorySummary(categories[1], BigDecimal("3430")),
                AnalyticsCategorySummary(categories[2], BigDecimal("4900")),
            ),
        typeSummaries = listOf(AnalyticsTypeSummary(AnalyticsType.Expenses, BigDecimal("18750"))),
    )
}

private fun previewTransaction(
    id: Int,
    account: Account,
    category: Category,
    amount: String,
    comment: String?,
): Transaction =
    Transaction(
        id = id,
        account = account,
        category = category,
        amount = BigDecimal(amount),
        transactionDate = PREVIEW_INSTANT,
        comment = comment,
        createdAt = PREVIEW_INSTANT,
        updatedAt = PREVIEW_INSTANT,
    )

private val PREVIEW_INSTANT: Instant = Instant.parse("2026-07-20T12:00:00Z")

private fun Transaction.toAnalyticsTransaction(): AnalyticsTransaction =
    AnalyticsTransaction(
        transaction = this,
        originalAmount = MoneyAmount(amount, currency),
        reportingAmount = MoneyAmount(amount, currency),
        rateDate = null,
    )
