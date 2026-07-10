package xyz.larkzhh.lime.data.network

import okhttp3.Interceptor
import okhttp3.Response
import xyz.larkzhh.lime.data.local.TokenStorage
import javax.inject.Inject

/**
 *  自定义网络请求认证拦截器
 */
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.accessToken
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")// 在求头添加 Bearer
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
