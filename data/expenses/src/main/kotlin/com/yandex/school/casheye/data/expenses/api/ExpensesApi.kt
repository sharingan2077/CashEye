package com.yandex.school.casheye.data.expenses.api

import com.yandex.school.casheye.data.expenses.dto.AccountDto
import com.yandex.school.casheye.data.expenses.dto.TransactionResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ExpensesApi {
    @GET("accounts")
    suspend fun getAccounts(): List<AccountDto>

    @GET("transactions/account/{accountId}/period")
    suspend fun getTransactions(
        @Path("accountId") accountId: Int,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): List<TransactionResponseDto>
}
