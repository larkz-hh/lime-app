package xyz.larkzhh.lime.data.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.TokenData
import javax.inject.Inject
import javax.inject.Named

/**
 *  自定义网络请求认证拦截器
 *  - 每次请求前自动携带 Access Token
 *  - Access Token 过期时自动用 Refresh Token 换取新 Token
 *  - Refresh Token 也失效时清除本地凭证
 */
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    @Named("base_url") private val baseUrl: String,
) : Interceptor {
    private val refreshClient by lazy { OkHttpClient() }
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        // 发请求前检查，过期则先刷新
        if (!tokenStorage.isAccessTokenValid() && !tokenStorage.refreshToken.isNullOrEmpty()) {
            refresh()
        }

        val response = chain.proceed(addAuthHeader(chain.request()))
   
        // 服务器返回 401/403，再刷新一次
        if (response.code in listOf(401, 403) && !tokenStorage.refreshToken.isNullOrEmpty()) {
            response.close()// 关闭旧响应
            return if (refresh()) {
                chain.proceed(addAuthHeader(chain.request()))
            } else {
                chain.proceed(chain.request()) // refresh 也失败，带空 token 发出
            }
        }

        return response
    }
    /// 添加 token
    private fun addAuthHeader(request: Request): Request {
        val token = tokenStorage.accessToken ?: return request
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    /// 同步刷新 Token，返回是否成功
    @Synchronized
    private fun refresh(): Boolean {
        // 双重检查
        if (tokenStorage.isAccessTokenValid()) return true

        val refreshToken = tokenStorage.refreshToken ?: return false

        val body = """{"refreshToken":"$refreshToken"}"""
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("${baseUrl}api/auth/refresh")
            .post(body)
            .build()

        return try {
            val response = refreshClient.newCall(request).execute()
            val responseStr = response.body?.string()
            if (response.isSuccessful && responseStr != null) {
                val type = object : TypeToken<ApiResponse<TokenData>>() {}.type
                val result: ApiResponse<TokenData> = gson.fromJson(responseStr, type)
                val data = result.data
                if (result.code == 200 && data != null) {
                    tokenStorage.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)
                    true
                } else {
                    false
                }
            } else {
                // 刷新令牌失效，清除凭证
                if (response.code == 401) tokenStorage.clearTokens()
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
