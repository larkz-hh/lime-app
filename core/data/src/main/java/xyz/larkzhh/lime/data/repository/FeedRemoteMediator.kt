package xyz.larkzhh.lime.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.gson.Gson
import xyz.larkzhh.lime.data.local.feed.FeedItemEntity
import xyz.larkzhh.lime.data.local.feed.FeedLocalDataSource
import xyz.larkzhh.lime.data.network.model.FeedResponse

/**
 * 信息流 RemoteMediator
 */
@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val feedKey: String,
    private val local: FeedLocalDataSource,
    private val gson: Gson,
    private val fetch: suspend (cursor: Long?) -> Result<FeedResponse>,
) : RemoteMediator<Int, FeedItemEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, FeedItemEntity>,
    ): MediatorResult {
        val cursor = when (loadType) {
            LoadType.REFRESH -> null
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> local.getCursor(feedKey)?.nextCursor
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            val response = fetch(cursor).getOrThrow()
            val startIndex = if (loadType == LoadType.REFRESH) 0
                else (state.lastItemOrNull()?.sortIndex ?: -1) + 1
            val entities = response.items.mapIndexed { i, item ->
                FeedItemEntity(
                    feedKey = feedKey,
                    noteId = item.id,
                    sortIndex = startIndex + i,
                    json = gson.toJson(item),
                )
            }

            if (loadType == LoadType.REFRESH) {
                local.replacePage(feedKey, entities, response.nextCursor)
            } else {
                local.appendPage(feedKey, entities, response.nextCursor)
            }

            MediatorResult.Success(endOfPaginationReached = response.nextCursor == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
