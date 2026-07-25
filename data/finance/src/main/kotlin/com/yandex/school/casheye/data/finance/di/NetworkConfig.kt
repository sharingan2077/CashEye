package com.yandex.school.casheye.data.finance.di

data class NetworkConfig(
    val apiKey: String,
    val baseUrl: String = "https://shmr-finance.ru/api/v1/",
    val isLoggingEnabled: Boolean = false,
)
