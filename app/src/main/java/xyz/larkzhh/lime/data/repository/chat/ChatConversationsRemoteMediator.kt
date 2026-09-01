package xyz.larkzhh.lime.data.repository.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import xyz.larkzhh.lime.data.local.chat.ChatLocalDataSource
import xyz.larkzhh.lime.data.local.chat.ConversationEntity
import xyz.larkzhh.lime.data.mapper.toEntity
import xyz.larkzhh.lime.data.network.chat.ChatRemoteDataSource

/**
 * 会话列表 RemoteMediator
 */
@OptIn(ExperimentalPagingApi::class)
class ChatConversationsRemoteMediator(
    private val remote: ChatRemoteDataSource,
    private val local: ChatLocalDataSource,
) : RemoteMediator<Int, ConversationEntity>() {

    private var nextCursor: String? = null
    private var endOfPagination = false

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ConversationEntity>,
    ): MediatorResult {
        return try {
            val cursor = when (loadType) {
                LoadType.REFRESH -> {
                    endOfPagination = false
                    null
                }

                LoadType.PREPEND ->
                    return MediatorResult.Success(endOfPaginationReached = true)

                LoadType.APPEND -> {
                    if (endOfPagination) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    nextCursor ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            val (items, newCursor) = remote.fetchConversations(cursor, PAGE_SIZE).getOrThrow()
            endOfPagination = newCursor == null
            nextCursor = newCursor

            if (loadType == LoadType.REFRESH) {
                local.replaceConversations(items.map { it.toEntity() })
            } else {
                local.upsertConversations(items.map { it.toEntity() })
            }

            MediatorResult.Success(endOfPaginationReached = endOfPagination)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
