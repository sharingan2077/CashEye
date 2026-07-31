package com.yandex.school.casheye.data.finance.network

import kotlinx.coroutines.delay
import retrofit2.HttpException
import kotlin.time.Duration.Companion.milliseconds

internal class ServerRetryPolicy(
    private val waitBeforeRetry: suspend (Long) -> Unit = { delay(it.milliseconds) },
) {
    suspend fun <T> execute(block: suspend () -> T): T {
        repeat(MAX_SERVER_ATTEMPTS - 1) {
            try {
                return block()
            } catch (error: HttpException) {
                if (error.code() !in SERVER_ERROR_RANGE) throw error
                waitBeforeRetry(RETRY_DELAY_MILLIS)
            }
        }
        return block()
    }

    private companion object {
        const val MAX_SERVER_ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 2_000L
        val SERVER_ERROR_RANGE = 500..599
    }
}
