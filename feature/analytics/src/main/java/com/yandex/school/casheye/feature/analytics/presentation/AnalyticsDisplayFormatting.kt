package com.yandex.school.casheye.feature.analytics.presentation

import com.yandex.school.casheye.core.designsystem.component.ErrorStateType
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import java.math.BigDecimal

internal fun formatAnalyticsDisplayAmount(
    amount: BigDecimal,
    amountType: AnalyticsType,
    selectedType: AnalyticsType,
    currencyCode: String,
): String =
    if (selectedType ==
        AnalyticsType.All
    ) {
        formatAnalyticsAmount(amount, amountType, currencyCode)
    } else {
        formatAmount(amount.abs(), currencyCode)
    }

internal fun formatAnalyticsAmount(
    amount: BigDecimal,
    type: AnalyticsType,
    currencyCode: String,
): String {
    val signedAmount = signedAnalyticsAmount(amount, type)
    val formatted = formatAmount(signedAmount, currencyCode)
    return if (signedAmount.signum() >= 0) "+$formatted" else formatted
}

internal fun signedAnalyticsAmount(
    amount: BigDecimal,
    type: AnalyticsType,
): BigDecimal =
    when (type) {
        AnalyticsType.Expenses -> amount.abs().negate()
        AnalyticsType.Income -> amount.abs()
        AnalyticsType.All -> amount
    }

internal fun FinanceFailureReason.toErrorStateType(): ErrorStateType =
    when (this) {
        FinanceFailureReason.Network -> ErrorStateType.Network
        FinanceFailureReason.Authorization -> ErrorStateType.Authorization
        FinanceFailureReason.Server -> ErrorStateType.Server
        FinanceFailureReason.Unknown -> ErrorStateType.Unknown
    }
