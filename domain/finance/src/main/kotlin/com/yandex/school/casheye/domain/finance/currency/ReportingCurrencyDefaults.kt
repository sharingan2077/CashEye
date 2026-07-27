package com.yandex.school.casheye.domain.finance.currency

import com.yandex.school.casheye.core.model.CurrencyCode

/**
 * Change this value to switch the reporting currency before the settings UI is available.
 * A previously saved user preference takes priority; clear the app data to test a changed default.
 */
val DEFAULT_REPORTING_CURRENCY: CurrencyCode = CurrencyCode.EUR
