package com.yandex.school.casheye.data.finance.api

import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FinanceApi {
    @GET("accounts")
    suspend fun getAccounts(): List<AccountDto>

    @GET("transactions/account/{accountId}/period")
    suspend fun getTransactions(
        @Path("accountId") accountId: Int,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): List<TransactionResponseDto>
}
