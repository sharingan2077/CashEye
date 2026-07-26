package com.yandex.school.casheye.data.finance.api

import com.yandex.school.casheye.data.finance.dto.ExchangeRateDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {
    @GET("v2/rates")
    suspend fun getRates(
        @Query("base") base: String = "EUR",
        @Query("quotes") quotes: String = "RUB,USD,GBP,CNY",
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<ExchangeRateDto>
}
