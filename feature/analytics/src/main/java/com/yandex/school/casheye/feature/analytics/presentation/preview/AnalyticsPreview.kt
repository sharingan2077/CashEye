@file:Suppress("MagicNumber")

package com.yandex.school.casheye.feature.analytics.presentation.preview

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

@Preview(
    name = "Details Light",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Details Dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AnalyticsDetailsPreview() {
    AnalyticsPreview(
        activeSheet = _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsSheet.Details,
    )
}

@Composable
private fun AnalyticsPreview(
    activeSheet: com.yandex.school.casheye.feature.analytics.presentation.AnalyticsSheet? = null,
) {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsScreen(
                state = analyticsPreviewState(activeSheet),
                onIntent = {},
            )
        }
    }
}

private fun analyticsPreviewState(
    activeSheet: com.yandex.school.casheye.feature.analytics.presentation.AnalyticsSheet?,
): com.yandex.school.casheye.feature.analytics.presentation.AnalyticsUiState.Content {
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
        _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsPeriod(
            startDate = LocalDate.of(2026, 7, 1),
            endDate = LocalDate.of(2026, 7, 20),
            preset = _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsPeriodPreset.Month,
        )
    val data =
        _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsScreenData(
            filters =
                _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsFilters(
                    type = _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsType.Expenses,
                    period = period,
                    accountId = accounts.first().id,
                ),
            currentDate = LocalDate.of(2026, 7, 20),
            accounts = accounts,
            categories = categories,
            activeSheet = activeSheet,
        )
    return _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsUiState.Content(
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
                _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsCategorySummary(
                    categories[0],
                    BigDecimal("10420"),
                ),
                _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsCategorySummary(
                    categories[1],
                    BigDecimal("3430"),
                ),
                _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsCategorySummary(
                    categories[2],
                    BigDecimal("4900"),
                ),
            ),
        typeSummaries =
            listOf(
                _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsTypeSummary(
                    _root_ide_package_.com.yandex.school.casheye.feature.analytics.presentation.AnalyticsType.Expenses,
                    BigDecimal("18750"),
                ),
            ),
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
