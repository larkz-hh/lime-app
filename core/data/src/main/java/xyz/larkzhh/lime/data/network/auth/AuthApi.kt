package xyz.larkzhh.lime.data.network.auth

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.ChangePasswordRequest
import xyz.larkzhh.lime.data.network.model.LoginRequest
import xyz.larkzhh.lime.data.network.model.RefreshTokenRequest
import xyz.larkzhh.lime.data.network.model.RegisterRequest
import xyz.larkzhh.lime.data.network.model.SendCodeRequest
import xyz.larkzhh.lime.data.network.model.TokenData

interface AuthApi {

    /// 发送验证码
    @POST("api/auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): ApiResponse<Unit>

    /// 登录
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<TokenData>

    /// 注册
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<Unit>

    /// 刷新令牌
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<TokenData>

    /// 登出
    @POST("api/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    /// 修改密码
    @PUT("api/user/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<TokenData>
}
