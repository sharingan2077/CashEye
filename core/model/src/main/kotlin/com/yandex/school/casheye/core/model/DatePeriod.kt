package com.yandex.school.casheye.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DatePeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

enum class DatePeriodPreset {
    Today,
    Week,
    Month,
    Quarter,
    Year,
    Custom,
}

fun DatePeriodPreset.resolve(today: LocalDate): DatePeriod =
    when (this) {
        DatePeriodPreset.Today -> {
            DatePeriod(today, today)
        }

        DatePeriodPreset.Week -> {
            DatePeriod(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today)
        }

        DatePeriodPreset.Month -> {
            DatePeriod(today.withDayOfMonth(1), today)
        }

        DatePeriodPreset.Quarter -> {
            DatePeriod(
                today.withMonth(((today.monthValue - 1) / 3) * 3 + 1).withDayOfMonth(1),
                today,
            )
        }

        DatePeriodPreset.Year -> {
            DatePeriod(today.withDayOfYear(1), today)
        }

        DatePeriodPreset.Custom -> {
            error("A custom period must be provided explicitly")
        }
    }
