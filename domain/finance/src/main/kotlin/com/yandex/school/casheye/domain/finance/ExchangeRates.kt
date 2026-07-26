package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

data class ExchangeRate(
    val baseCurrency: CurrencyCode,
    val quoteCurrency: CurrencyCode,
    val rate: BigDecimal,
    val date: LocalDate,
)

data class ExchangeRateSnapshot(
    val rates: List<ExchangeRate>,
    val requestedFrom: LocalDate?,
    val requestedTo: LocalDate?,
    val missingCurrencies: Set<CurrencyCode>,
) {
    val isComplete: Boolean
        get() = missingCurrencies.isEmpty()
}

sealed interface ExchangeRateRefreshResult {
    data object Fresh : ExchangeRateRefreshResult

    data object Updated : ExchangeRateRefreshResult

    data class TemporaryFailure(
        val cachedDataAvailable: Boolean,
        val cause: Throwable,
    ) : ExchangeRateRefreshResult

    data class PermanentFailure(
        val cachedDataAvailable: Boolean,
        val cause: Throwable?,
    ) : ExchangeRateRefreshResult

    data class Incomplete(
        val missingCurrencies: Set<CurrencyCode>,
    ) : ExchangeRateRefreshResult
}

interface ExchangeRateRepository {
    fun observeLatest(): Flow<ExchangeRateSnapshot>

    fun observeRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<ExchangeRateSnapshot>

    suspend fun refreshLatest(force: Boolean = false): ExchangeRateRefreshResult

    suspend fun refreshRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): ExchangeRateRefreshResult
}

sealed interface CurrencyConversionResult {
    data class Complete(
        val money: MoneyAmount,
        val sourceRateDate: LocalDate?,
        val targetRateDate: LocalDate?,
    ) : CurrencyConversionResult

    data class Incomplete(
        val source: CurrencyCode,
        val target: CurrencyCode,
        val requestedDate: LocalDate,
        val missingCurrencies: Set<CurrencyCode>,
    ) : CurrencyConversionResult
}

class CurrencyConverter(
    private val mathContext: MathContext = MathContext(34, RoundingMode.HALF_EVEN),
) {
    fun convert(
        money: MoneyAmount,
        target: CurrencyCode,
        date: LocalDate,
        rates: Collection<ExchangeRate>,
    ): CurrencyConversionResult {
        if (money.currency == target) {
            return CurrencyConversionResult.Complete(
                money = money,
                sourceRateDate = null,
                targetRateDate = null,
            )
        }

        val sourceRate = findRate(money.currency, date, rates)
        val targetRate = findRate(target, date, rates)
        val missing =
            buildSet {
                if (sourceRate == null) add(money.currency)
                if (targetRate == null) add(target)
            }
        if (missing.isNotEmpty()) {
            return CurrencyConversionResult.Incomplete(
                source = money.currency,
                target = target,
                requestedDate = date,
                missingCurrencies = missing,
            )
        }

        val sourceValue = requireNotNull(sourceRate)
        val targetValue = requireNotNull(targetRate)
        val converted =
            money.amount
                .multiply(targetValue.rate, mathContext)
                .divide(sourceValue.rate, mathContext)
        return CurrencyConversionResult.Complete(
            money = MoneyAmount(converted, target),
            sourceRateDate = sourceValue.date.takeUnless { money.currency == CurrencyCode.EUR },
            targetRateDate = targetValue.date.takeUnless { target == CurrencyCode.EUR },
        )
    }

    private fun findRate(
        currency: CurrencyCode,
        date: LocalDate,
        rates: Collection<ExchangeRate>,
    ): ExchangeRate? {
        if (currency == CurrencyCode.EUR) {
            return ExchangeRate(
                baseCurrency = CurrencyCode.EUR,
                quoteCurrency = CurrencyCode.EUR,
                rate = BigDecimal.ONE,
                date = date,
            )
        }
        return rates
            .asSequence()
            .filter {
                it.baseCurrency == CurrencyCode.EUR &&
                    it.quoteCurrency == currency &&
                    !it.date.isAfter(date)
            }.maxByOrNull(ExchangeRate::date)
    }
}
