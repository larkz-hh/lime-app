package xyz.larkzhh.lime.domain.repository

import kotlinx.coroutines.flow.StateFlow
import xyz.larkzhh.lime.data.network.model.FollowListResponse
import xyz.larkzhh.lime.domain.model.FollowRelation

/**
 * 关注相关数据仓库
 */
interface FollowRepository {

    /// 关注关系映射
    val relations: StateFlow<Map<Long, FollowRelation>>

    /// 写入、更新用户关注关系
    fun updateRelation(userId: Long, relation: FollowRelation?)

    /// 关注用户
    suspend fun follow(userId: Long): Result<Unit>

    /// 取消关注
    suspend fun unfollow(userId: Long): Result<Unit>

    /// 关注列表
    suspend fun getFollowing(userId: Long, cursor: Long?, size: Int = 20): Result<FollowListResponse>

    /// 粉丝列表
    suspend fun getFollowers(userId: Long, cursor: Long?, size: Int = 20): Result<FollowListResponse>
}
