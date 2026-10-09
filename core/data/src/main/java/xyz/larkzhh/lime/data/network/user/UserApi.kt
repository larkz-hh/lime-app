package xyz.larkzhh.lime.data.network.user

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.FollowListResponse
import xyz.larkzhh.lime.data.network.model.UpdateProfileRequest
import xyz.larkzhh.lime.data.network.model.UserData

interface UserApi {

    /// 获取当前用户信息
    @GET("api/user/me")
    suspend fun getMe(): ApiResponse<UserData>

    /// 获取指定用户公开资料
    @GET("api/user/{userId}")
    suspend fun getUserById(@Path("userId") userId: Long): ApiResponse<UserData>

    /// handle id 获取指定用户公开资料
    @GET("api/user/byHandle/{handle}")
    suspend fun getUserByHandle(@Path("handle") handle: String): ApiResponse<UserData>

    /// uid 获取指定用户公开资料
    @GET("api/user/byUid/{uid}")
    suspend fun getUserByUid(@Path("uid") uid: String): ApiResponse<UserData>

    /// 关注用户
    @POST("api/user/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: Long): ApiResponse<Unit>

    /// 取消关注
    @DELETE("api/user/{userId}/follow")
    suspend fun unfollowUser(@Path("userId") userId: Long): ApiResponse<Unit>

    /// 关注列表
    @GET("api/user/{userId}/following")
    suspend fun getFollowing(
        @Path("userId") userId: Long,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<FollowListResponse>

    /// 粉丝列表
    @GET("api/user/{userId}/followers")
    suspend fun getFollowers(
        @Path("userId") userId: Long,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<FollowListResponse>

    /// 修改个人资料
    @PUT("api/user/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): ApiResponse<UserData>

    /// 上传、更换头像
    @Multipart
    @POST("api/user/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): ApiResponse<UserData>

    /// 互关好友列表
    @GET("api/user/mutual-friends")
    suspend fun getMutualFriends(): ApiResponse<List<UserData>>

    /// 上传群头像
    @Multipart
    @POST("api/group/avatar")
    suspend fun uploadGroupAvatar(@Part file: MultipartBody.Part): ApiResponse<String>

    /// 上传、更换背景图
    @Multipart
    @POST("api/user/me/background")
    suspend fun uploadBackground(@Part file: MultipartBody.Part): ApiResponse<UserData>
}
