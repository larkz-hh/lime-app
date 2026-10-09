package xyz.larkzhh.lime.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.random.Random

/**
 * 弱网自动重试拦截器
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 非幂等请求直接放行
        if (!request.method.isIdempotent()) {
            return chain.proceed(request)
        }

        var attempt = 0
        while (true) {
            attempt++
            try {
                return chain.proceed(request)
            } catch (e: IOException) {
                if (attempt > maxRetries) throw e
            }
            val backoff = BASE_DELAY_MS shl (attempt - 1)
            Thread.sleep(backoff + Random.nextLong(0, backoff + 1))
        }
    }

    private fun String.isIdempotent(): Boolean = this == "GET" || this == "HEAD"

    private companion object {
        const val BASE_DELAY_MS = 500L
    }
}
