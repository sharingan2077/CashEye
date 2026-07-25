package com.yandex.school.casheye.data.finance.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ServerRetryPolicyTest {
    @Test
    fun `500 500 200 uses three attempts and two delays`() =
        runBlocking {
            var attempts = 0
            val delays = mutableListOf<Long>()
            val policy = ServerRetryPolicy { delays += it }

            val result =
                policy.execute {
                    attempts += 1
                    if (attempts < 3) throw httpException(500)
                    "ok"
                }

            assertEquals("ok", result)
            assertEquals(3, attempts)
            assertEquals(listOf(2_000L, 2_000L), delays)
        }

    @Test
    fun `three server errors stop after third attempt`() {
        var attempts = 0
        val policy = ServerRetryPolicy {}

        assertThrows(HttpException::class.java) {
            runBlocking {
                policy.execute {
                    attempts += 1
                    throw httpException(500)
                }
            }
        }

        assertEquals(3, attempts)
    }

    @Test
    fun `client error is not retried`() {
        var attempts = 0
        val policy = ServerRetryPolicy {}

        assertThrows(HttpException::class.java) {
            runBlocking {
                policy.execute {
                    attempts += 1
                    throw httpException(400)
                }
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `io exception is not retried`() {
        var attempts = 0
        val policy = ServerRetryPolicy {}

        assertThrows(IOException::class.java) {
            runBlocking {
                policy.execute {
                    attempts += 1
                    throw IOException("offline")
                }
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `cancellation is propagated`() {
        val policy = ServerRetryPolicy {}

        assertThrows(CancellationException::class.java) {
            runBlocking {
                policy.execute {
                    throw CancellationException("cancel")
                }
            }
        }
    }
}

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Unit>(code, "error".toResponseBody()))
