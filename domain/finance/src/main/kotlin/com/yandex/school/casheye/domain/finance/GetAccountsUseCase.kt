package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.domain.finance.currency.CurrencyConversionResult
import com.yandex.school.casheye.domain.finance.currency.CurrencyConverter
import com.yandex.school.casheye.domain.finance.currency.DefaultReportingCurrencyRepository
import com.yandex.school.casheye.domain.finance.currency.EmptyExchangeRateRepository
import com.yandex.school.casheye.domain.finance.currency.ExchangeRate
import com.yandex.school.casheye.domain.finance.currency.ExchangeRateRepository
import com.yandex.school.casheye.domain.finance.currency.ReportingCurrencyRepository
import com.yandex.school.casheye.domain.finance.currency.aggregateNativeMoney
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.LocalDate

class GetAccountsUseCase(
    private val repository: FinanceRepository,
    private val reportingCurrencyRepository: ReportingCurrencyRepository = DefaultReportingCurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository = EmptyExchangeRateRepository,
    private val currencyConverter: CurrencyConverter = CurrencyConverter(),
) {
    operator fun invoke(): Flow<AccountsLoadResult> =
        combine(
            repository.observeAccounts(),
            reportingCurrencyRepository.observe(),
            exchangeRateRepository.observeLatest(),
        ) { accounts, reportingCurrency, rateSnapshot ->
            val nativeTotals =
                aggregateNativeMoney(
                    amounts = accounts.map { MoneyAmount(it.balance, it.currency) },
                    reportingCurrency = reportingCurrency,
                )
            val result: AccountsLoadResult =
                AccountsLoadResult.Success(
                    AccountsSummary(
                        nativeTotals = nativeTotals,
                        currentValuation =
                            nativeTotals
                                .takeIf { it.isNotEmpty() }
                                ?.let {
                                    calculateCurrentValuation(
                                        nativeTotals = it,
                                        reportingCurrency = reportingCurrency,
                                        rates = rateSnapshot.rates,
                                    )
                                },
                        accounts = accounts,
                    ),
                )
            result
        }.catch { emit(AccountsLoadResult.Failure(FinanceFailureReason.Unknown)) }

    suspend fun refresh(): FinanceRefreshResult = repository.refreshAccounts()

    private fun calculateCurrentValuation(
        nativeTotals: List<MoneyAmount>,
        reportingCurrency: CurrencyCode,
        rates: List<ExchangeRate>,
    ): AccountsCurrentValuation {
        var included = BigDecimal.ZERO
        var hasIncludedAmount = false
        val excluded = mutableListOf<MoneyAmount>()
        val rateDates = mutableListOf<LocalDate>()

        nativeTotals.forEach { nativeTotal ->
            when (
                val conversion =
                    currencyConverter.convert(
                        money = nativeTotal,
                        target = reportingCurrency,
                        date = LocalDate.MAX,
                        rates = rates,
                    )
            ) {
                is CurrencyConversionResult.Complete -> {
                    included += conversion.money.amount
                    hasIncludedAmount = true
                    conversion.sourceRateDate?.let(rateDates::add)
                    conversion.targetRateDate?.let(rateDates::add)
                }

                is CurrencyConversionResult.Incomplete -> {
                    excluded += nativeTotal
                }
            }
        }

        return AccountsCurrentValuation(
            includedTotal =
                included
                    .takeIf { hasIncludedAmount }
                    ?.let { MoneyAmount(it, reportingCurrency) },
            excludedNativeTotals = excluded,
            rateDate = rateDates.minOrNull(),
        )
    }
}
