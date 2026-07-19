package xyz.larkzhh.lime.data.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.LoginRequest
import xyz.larkzhh.lime.data.network.model.RefreshTokenRequest
import xyz.larkzhh.lime.data.network.model.RegisterRequest
import xyz.larkzhh.lime.data.network.model.SendCodeRequest
import xyz.larkzhh.lime.data.network.model.TokenData
import xyz.larkzhh.lime.data.network.model.UpdateProfileRequest
import xyz.larkzhh.lime.data.network.model.UserData

/**
 * 认证 API 接口
 */
interface ApiService {

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

    /// 获取当前用户信息
    @GET("api/user/me")
    suspend fun getMe(): ApiResponse<UserData>

    /// 修改个人资料
    @PUT("api/user/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): ApiResponse<UserData>

    /// 上传、更换头像
    @Multipart
    @POST("api/user/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): ApiResponse<UserData>

    /// 上传、更换背景图
    @Multipart
    @POST("api/user/me/background")
    suspend fun uploadBackground(@Part file: MultipartBody.Part): ApiResponse<UserData>
}
