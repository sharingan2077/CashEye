package com.yandex.school.casheye.data.finance.di

import com.yandex.school.casheye.data.finance.api.FinanceApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@BindingContainer
object FinanceNetworkBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @SingleIn(AppScope::class)
    fun provideOkHttpClient(config: NetworkConfig): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    val request =
                        chain
                            .request()
                            .newBuilder()
                            .header("Authorization", "Bearer ${config.apiKey}")
                            .build()
                    chain.proceed(request)
                }

        if (config.isLoggingEnabled) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    redactHeader("Authorization")
                    level = HttpLoggingInterceptor.Level.HEADERS
                },
            )
        }

        return builder.build()
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideRetrofit(
        config: NetworkConfig,
        client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @SingleIn(AppScope::class)
    fun provideFinanceApi(retrofit: Retrofit): FinanceApi = retrofit.create(FinanceApi::class.java)
}
