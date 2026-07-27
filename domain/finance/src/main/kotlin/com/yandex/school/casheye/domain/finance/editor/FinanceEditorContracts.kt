package com.yandex.school.casheye.domain.finance.editor

import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import java.math.BigDecimal
import java.time.Instant

sealed interface EditorResult<out T> {
    data class Success<T>(
        val value: T,
    ) : EditorResult<T>

    data class Failure(
        val reason: FinanceFailureReason,
    ) : EditorResult<Nothing>
}

data class SaveTransactionCommand(
    val id: Int?,
    val accountId: Int,
    val categoryId: Int,
    val amount: BigDecimal,
    val transactionDate: Instant,
    val comment: String?,
)

data class SaveAccountCommand(
    val id: Int?,
    val name: String,
    val emoji: String,
    val balance: BigDecimal,
    val currency: CurrencyCode,
) {
    constructor(
        id: Int?,
        name: String,
        emoji: String,
        balance: BigDecimal,
        currency: String,
    ) : this(id, name, emoji, balance, CurrencyCode.fromIsoCode(currency))
}
