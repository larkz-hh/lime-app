package xyz.larkzhh.lime.data.repository

import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.FollowListResponse
import xyz.larkzhh.lime.data.network.model.UpdateProfileRequest
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.data.network.user.UserApi
import xyz.larkzhh.lime.domain.model.FollowRelation

/// 关注仓库
class FollowRepositoryImplTest {

    private val api = FakeUserApi()
    private val repository = FollowRepositoryImpl(api)

    @Test
    fun 关注成功后写入新关系() = runBlocking {
        api.followResponse = ApiResponse(200, "ok", null)

        val result = repository.follow(9L)

        assertTrue(result.isSuccess)
        assertEquals(FollowRelation(following = true, followedBack = false), repository.relations.value[9L])
    }

    @Test
    fun 关注保留已有的回关状态() = runBlocking {
        api.followResponse = ApiResponse(200, "ok", null)
        repository.updateRelation(9L, FollowRelation(following = false, followedBack = true))

        repository.follow(9L)

        assertEquals(FollowRelation(following = true, followedBack = true), repository.relations.value[9L])
    }

    @Test
    fun 取消关注保留已有的回关状态() = runBlocking {
        api.unfollowResponse = ApiResponse(200, "ok", null)
        repository.updateRelation(9L, FollowRelation(following = true, followedBack = true))

        repository.unfollow(9L)

        assertEquals(FollowRelation(following = false, followedBack = true), repository.relations.value[9L])
    }

    @Test
    fun 关注失败不写关系() = runBlocking {
        api.followResponse = ApiResponse(500, "服务器错误", null)

        val result = repository.follow(9L)

        assertTrue(result.isFailure)
        assertNull(repository.relations.value[9L])
    }

    @Test
    fun 关注失败带上服务端消息() = runBlocking {
        api.followResponse = ApiResponse(403, "对方已拉黑你", null)

        assertEquals("对方已拉黑你", repository.follow(9L).exceptionOrNull()?.message)
    }

    @Test
    fun 取消关注失败不改动已有关系() = runBlocking {
        api.unfollowResponse = ApiResponse(500, "服务器错误", null)
        repository.updateRelation(9L, FollowRelation(following = true, followedBack = true))

        val result = repository.unfollow(9L)

        assertTrue(result.isFailure)
        assertEquals(FollowRelation(following = true, followedBack = true), repository.relations.value[9L])
    }

    @Test
    fun 关系置空表示移除条目() {
        repository.updateRelation(9L, FollowRelation(following = true, followedBack = false))

        repository.updateRelation(9L, null)

        assertNull(repository.relations.value[9L])
    }

    @Test
    fun 更新一个用户不影响其他用户() {
        repository.updateRelation(1L, FollowRelation(following = true, followedBack = false))
        repository.updateRelation(2L, FollowRelation(following = true, followedBack = false))

        repository.updateRelation(1L, null)

        assertNull(repository.relations.value[1L])
        assertEquals(FollowRelation(following = true, followedBack = false), repository.relations.value[2L])
    }

    @Test
    fun 关注列表透传分页参数并返回数据() = runBlocking {
        api.followingResponse =
            ApiResponse(200, "ok", FollowListResponse(items = emptyList(), nextCursor = "100", hasMore = true))

        val result = repository.getFollowing(userId = 1L, cursor = 100L, size = 20)

        assertEquals("100", result.getOrThrow().nextCursor)
        assertEquals(100L, api.lastCursor)
        assertEquals(20, api.lastSize)
    }

    @Test
    fun 粉丝列表非200时失败() = runBlocking {
        api.followersResponse = ApiResponse(404, "用户不存在", null)

        val result = repository.getFollowers(userId = 1L, cursor = null, size = 20)

        assertEquals("用户不存在", result.exceptionOrNull()?.message)
    }

    private class FakeUserApi : UserApi {
        var followResponse: ApiResponse<Unit> = ApiResponse(200, "ok", null)
        var unfollowResponse: ApiResponse<Unit> = ApiResponse(200, "ok", null)
        var followingResponse: ApiResponse<FollowListResponse> = ApiResponse(200, "ok", null)
        var followersResponse: ApiResponse<FollowListResponse> = ApiResponse(200, "ok", null)
        var lastCursor: Long? = null
        var lastSize: Int = -1

        override suspend fun followUser(userId: Long): ApiResponse<Unit> = followResponse

        override suspend fun unfollowUser(userId: Long): ApiResponse<Unit> = unfollowResponse

        override suspend fun getFollowing(userId: Long, cursor: Long?, size: Int): ApiResponse<FollowListResponse> {
            lastCursor = cursor
            lastSize = size
            return followingResponse
        }

        override suspend fun getFollowers(userId: Long, cursor: Long?, size: Int): ApiResponse<FollowListResponse> =
            followersResponse

        override suspend fun getMe(): ApiResponse<UserData> = error("未使用")

        override suspend fun getUserById(userId: Long): ApiResponse<UserData> = error("未使用")

        override suspend fun getUserByHandle(handle: String): ApiResponse<UserData> = error("未使用")

        override suspend fun getUserByUid(uid: String): ApiResponse<UserData> = error("未使用")

        override suspend fun updateMe(request: UpdateProfileRequest): ApiResponse<UserData> = error("未使用")

        override suspend fun uploadAvatar(file: MultipartBody.Part): ApiResponse<UserData> = error("未使用")

        override suspend fun getMutualFriends(): ApiResponse<List<UserData>> = error("未使用")

        override suspend fun uploadGroupAvatar(file: MultipartBody.Part): ApiResponse<String> = error("未使用")

        override suspend fun uploadBackground(file: MultipartBody.Part): ApiResponse<UserData> = error("未使用")
    }
}
