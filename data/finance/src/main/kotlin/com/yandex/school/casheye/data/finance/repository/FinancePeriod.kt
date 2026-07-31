package com.yandex.school.casheye.data.finance.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class FinancePeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val start: Instant,
    val end: Instant,
)

internal fun LocalDate.toFinancePeriod(endDate: LocalDate): FinancePeriod {
    val zone = ZoneId.systemDefault()
    return FinancePeriod(
        startDate = this,
        endDate = endDate,
        start = atStartOfDay(zone).toInstant(),
        end =
            endDate
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .minusMillis(1),
    )
}
