package xyz.larkzhh.lime.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.local.feed.FeedCursorEntity
import xyz.larkzhh.lime.data.local.feed.FeedItemEntity
import xyz.larkzhh.lime.data.local.feed.FeedLocalDataSource
import xyz.larkzhh.lime.data.network.model.FeedAuthor
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.data.network.model.FeedResponse

/// 信息流 RemoteMediator
@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediatorTest {

    private val local = mockk<FeedLocalDataSource>(relaxed = true)
    private val feedKey = "home"

    private fun mediator(fetch: suspend (Long?) -> Result<FeedResponse>) =
        FeedRemoteMediator(feedKey = feedKey, local = local, gson = Gson(), fetch = fetch)

    private fun stateOf(vararg items: FeedItemEntity): PagingState<Int, FeedItemEntity> = PagingState(
        pages = if (items.isEmpty()) {
            emptyList()
        } else {
            listOf(PagingSource.LoadResult.Page(data = items.toList(), prevKey = null, nextKey = 1))
        },
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0,
    )

    private fun entity(sortIndex: Int) = FeedItemEntity(
        feedKey = feedKey,
        noteId = sortIndex.toLong(),
        sortIndex = sortIndex,
        json = "{}",
    )

    private fun item(id: Long) = FeedItem(
        id = id,
        title = "标题$id",
        coverImage = null,
        likeCount = 0,
        liked = false,
        author = FeedAuthor(id = 1L, nickname = "作者", avatar = null),
    )

    @Test
    fun 刷新时游标为空且索引从0开始() = runBlocking {
        var requestedCursor: Long? = -1L
        var saved: List<FeedItemEntity>? = null
        coEvery { local.replacePage(feedKey, any(), any()) } answers {
            saved = secondArg()
            Unit
        }
        val mediator = mediator { cursor ->
            requestedCursor = cursor
            Result.success(FeedResponse(listOf(item(11L), item(12L)), nextCursor = 99L, hasMore = true))
        }

        val result = mediator.load(LoadType.REFRESH, stateOf())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertNull(requestedCursor)
        assertEquals(listOf(0, 1), saved?.map { it.sortIndex })
        assertEquals(listOf(11L, 12L), saved?.map { it.noteId })
    }

    @Test
    fun 刷新时没有下一页则标记结束() = runBlocking {
        val mediator = mediator { Result.success(FeedResponse(emptyList(), nextCursor = null, hasMore = false)) }

        val result = mediator.load(LoadType.REFRESH, stateOf())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun 追加时没有本地游标则直接结束且不请求网络() = runBlocking {
        var fetchCalls = 0
        coEvery { local.getCursor(feedKey) } returns null
        val mediator = mediator { fetchCalls++; Result.success(FeedResponse(emptyList(), null, false)) }

        val result = mediator.load(LoadType.APPEND, stateOf(entity(0)))

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, fetchCalls)
        coVerify(exactly = 0) { local.appendPage(any(), any(), any()) }
    }

    @Test
    fun 追加时从最后一条索引之后继续并走追加分支() = runBlocking {
        var requestedCursor: Long? = -1L
        var saved: List<FeedItemEntity>? = null
        coEvery { local.getCursor(feedKey) } returns FeedCursorEntity(feedKey, nextCursor = 42L)
        coEvery { local.appendPage(feedKey, any(), any()) } answers {
            saved = secondArg()
            Unit
        }
        val mediator = mediator { cursor ->
            requestedCursor = cursor
            Result.success(FeedResponse(listOf(item(21L)), nextCursor = null, hasMore = false))
        }

        val result = mediator.load(LoadType.APPEND, stateOf(entity(5), entity(6)))

        assertEquals(42L, requestedCursor)
        assertEquals(listOf(7), saved?.map { it.sortIndex })
        assertEquals(listOf(21L), saved?.map { it.noteId })
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        coVerify(exactly = 0) { local.replacePage(any(), any(), any()) }
    }

    @Test
    fun 向前加载直接结束且不请求网络() = runBlocking {
        var fetchCalls = 0
        val mediator = mediator { fetchCalls++; Result.success(FeedResponse(emptyList(), null, false)) }

        val result = mediator.load(LoadType.PREPEND, stateOf())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, fetchCalls)
    }

    @Test
    fun 请求失败时返回Error() = runBlocking {
        val mediator = mediator { Result.failure(RuntimeException("断网了")) }

        val result = mediator.load(LoadType.REFRESH, stateOf())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals("断网了", (result as RemoteMediator.MediatorResult.Error).throwable.message)
    }
}
