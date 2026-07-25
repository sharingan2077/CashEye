package com.yandex.school.casheye.data.finance.api

import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.AccountRequestDto
import com.yandex.school.casheye.data.finance.dto.AccountResponseDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionRequestDto
import com.yandex.school.casheye.data.finance.dto.TransactionDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FinanceApi {
    @GET("accounts")
    suspend fun getAccounts(): List<AccountDto>

    @GET("accounts/{id}")
    suspend fun getAccount(
        @Path("id") id: Int,
    ): AccountResponseDto

    @POST("accounts")
    suspend fun createAccount(
        @Body request: AccountRequestDto,
    ): AccountDto

    @PUT("accounts/{id}")
    suspend fun updateAccount(
        @Path("id") id: Int,
        @Body request: AccountRequestDto,
    ): AccountDto

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(
        @Path("id") id: Int,
    )

    @GET("categories/type/{isIncome}")
    suspend fun getCategories(
        @Path("isIncome") isIncome: Boolean,
    ): List<CategoryDto>

    @GET("transactions/{id}")
    suspend fun getTransaction(
        @Path("id") id: Int,
    ): TransactionResponseDto

    @POST("transactions")
    suspend fun createTransaction(
        @Body request: TransactionRequestDto,
    ): TransactionDto

    @PUT("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Int,
        @Body request: TransactionRequestDto,
    ): TransactionResponseDto

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: Int,
    )

    @GET("transactions/account/{accountId}/period")
    suspend fun getTransactions(
        @Path("accountId") accountId: Int,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): List<TransactionResponseDto>
}
