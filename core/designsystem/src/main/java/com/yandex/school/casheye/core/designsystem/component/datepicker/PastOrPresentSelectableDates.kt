package com.yandex.school.casheye.core.designsystem.component.datepicker

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberPastOrPresentSelectableDates(currentDate: LocalDate = LocalDate.now()): SelectableDates =
    remember(currentDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= currentDate

            override fun isSelectableYear(year: Int): Boolean = year <= currentDate.year
        }
    }
