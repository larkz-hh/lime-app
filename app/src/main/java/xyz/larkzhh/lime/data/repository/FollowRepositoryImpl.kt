  package xyz.larkzhh.lime.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.FollowListResponse
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.repository.FollowRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 关注仓库实现
 */
@Singleton
class FollowRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : FollowRepository {

    private val _relations = MutableStateFlow<Map<Long, FollowRelation>>(emptyMap())
    override val relations: StateFlow<Map<Long, FollowRelation>> = _relations.asStateFlow()

    /// 写入、更新用户关注关系
    override fun updateRelation(userId: Long, relation: FollowRelation?) {
        _relations.update { current ->
            if (relation == null) current - userId else current + (userId to relation)
        }
    }

     /// 关注用户
    override suspend fun follow(userId: Long): Result<Unit> = runCatching {
        val response = apiService.followUser(userId)
        check(response.code == 200) { response.message }
        val prev = _relations.value[userId]
        _relations.update { it + (userId to FollowRelation(true, prev?.followedBack ?: false)) }
    }

    /// 取消关注
    override suspend fun unfollow(userId: Long): Result<Unit> = runCatching {
        val response = apiService.unfollowUser(userId)
        check(response.code == 200) { response.message }
        val prev = _relations.value[userId]
        _relations.update { it + (userId to FollowRelation(false, prev?.followedBack ?: false)) }
    }

    /// 获取关注列表
    override suspend fun getFollowing(userId: Long, cursor: Long?, size: Int): Result<FollowListResponse> =
        runCatching {
            val response = apiService.getFollowing(userId, cursor, size)
            check(response.code == 200 && response.data != null) { response.message }
            response.data
        }

    /// 获取粉丝列表
    override suspend fun getFollowers(userId: Long, cursor: Long?, size: Int): Result<FollowListResponse> =
        runCatching {
            val response = apiService.getFollowers(userId, cursor, size)
            check(response.code == 200 && response.data != null) { response.message }
            response.data
        }
}
