package xyz.larkzhh.lime.data.network.im

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.ConversationOpenData
import xyz.larkzhh.lime.data.network.model.ConversationOpenRequest
import xyz.larkzhh.lime.data.network.model.ImUserSigData

interface ImApi {

    /// 获取 IM UserSig
    @GET("api/im/userSig")
    suspend fun getImUserSig(): ApiResponse<ImUserSigData>

    /// 打开私信会话
    @POST("api/im/conversation/open")
    suspend fun openConversation(@Body request: ConversationOpenRequest): ApiResponse<ConversationOpenData>
}
