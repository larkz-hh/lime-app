package xyz.larkzhh.lime.data.repository

import android.content.Context
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.local.feed.FeedItemEntity
import xyz.larkzhh.lime.data.local.feed.FeedLocalDataSource
import xyz.larkzhh.lime.data.local.note.NoteCacheLocalDataSource
import xyz.larkzhh.lime.data.network.model.FeedAuthor
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.NoteImageData
import xyz.larkzhh.lime.data.network.note.NoteRemoteDataSource

/// 笔记仓库
class NoteRepositoryImplTest {

    private val feedLocal = mockk<FeedLocalDataSource>(relaxed = true)
    private val noteCache = mockk<NoteCacheLocalDataSource>(relaxed = true)
    private val remote = mockk<NoteRemoteDataSource>(relaxed = true)
    private val gson = Gson()

    private val repository = NoteRepositoryImpl(
        context = mockk<Context>(relaxed = true),
        feedLocalDataSource = feedLocal,
        noteCacheLocalDataSource = noteCache,
        noteRemoteDataSource = remote,
        gson = gson,
    )

    private val author = FeedAuthor(id = 1L, nickname = "作者", avatar = null)

    private fun detail(
        id: Long = 1L,
        imageUrl: String? = null,
        imageWidth: Int? = null,
        imageHeight: Int? = null,
        noteType: Int = 1,
    ) = NoteDetailData(
        id = id,
        title = "新标题",
        content = "正文",
        status = 1,
        images = listOfNotNull(
            imageUrl?.let { NoteImageData(id = 7L, url = it, sortOrder = 0, width = imageWidth, height = imageHeight) },
        ),
        likeCount = 2,
        favCount = 3,
        viewCount = 4,
        liked = false,
        favorited = false,
        author = author,
        noteType = noteType,
    )

    private fun oldFeedItem(
        id: Long = 1L,
        coverImage: String? = "old.jpg",
        coverWidth: Int? = 100,
        coverHeight: Int? = 200,
        viewCount: Int? = 77,
    ) = FeedItem(
        id = id,
        title = "旧标题",
        coverImage = coverImage,
        coverWidth = coverWidth,
        coverHeight = coverHeight,
        likeCount = 1,
        liked = false,
        author = author,
        viewCount = viewCount,
    )

    private fun cacheOldFeedItem(item: FeedItem) {
        coEvery { feedLocal.getNoteItems(item.id) } returns listOf(
            FeedItemEntity(feedKey = "k", noteId = item.id, sortIndex = 0, json = gson.toJson(item)),
        )
    }

    @Test
    fun 详情拉取成功后写入本地缓存() = runBlocking {
        val data = detail()
        coEvery { remote.getNoteDetail(1L, false) } returns Result.success(data)

        val result = repository.getNoteDetail(1L, noView = false)

        assertEquals(data, result.getOrThrow())
        coVerify(exactly = 1) { noteCache.saveNoteDetail(1L, data) }
    }

    @Test
    fun 详情拉取失败不写本地缓存() = runBlocking {
        coEvery { remote.getNoteDetail(1L, false) } returns Result.failure(RuntimeException("没有这条笔记"))

        val result = repository.getNoteDetail(1L, noView = false)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { noteCache.saveNoteDetail(any(), any()) }
    }

    @Test
    fun 删除成功后同时清详情缓存和信息流快照() = runBlocking {
        coEvery { remote.deleteNote(1L) } returns Result.success(Unit)

        val result = repository.deleteNote(1L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { noteCache.deleteNoteCache(1L) }
        coVerify(exactly = 1) { feedLocal.deleteByNoteId(1L) }
    }

    @Test
    fun 删除失败时不碰缓存() = runBlocking {
        coEvery { remote.deleteNote(1L) } returns Result.failure(RuntimeException("无权删除"))

        val result = repository.deleteNote(1L)

        assertEquals("无权删除", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { noteCache.deleteNoteCache(any()) }
        coVerify(exactly = 0) { feedLocal.deleteByNoteId(any()) }
    }

    @Test
    fun 编辑成功后按新详情刷新信息流快照() = runBlocking {
        cacheOldFeedItem(oldFeedItem())
        val json = slot<String>()
        coEvery { remote.updateNote(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { remote.getNoteDetail(1L, true) } returns
            Result.success(detail(imageUrl = "new.jpg", imageWidth = 1080, imageHeight = 1440))
        coEvery { feedLocal.updateNoteItem(1L, capture(json)) } returns Unit

        val result = repository.updateNote(1L, "标题", "正文", listOf("new.jpg"), null, 1)

        assertTrue(result.isSuccess)
        val updated = gson.fromJson(json.captured, FeedItem::class.java)
        assertEquals("new.jpg", updated.coverImage)
        assertEquals(1080, updated.coverWidth)
        assertEquals(1440, updated.coverHeight)
        assertEquals(77, updated.viewCount)
    }

    @Test
    fun 新封面尺寸为0时沿用旧尺寸() = runBlocking {
        cacheOldFeedItem(oldFeedItem(coverWidth = 100, coverHeight = 200))
        val json = slot<String>()
        coEvery { remote.updateNote(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { remote.getNoteDetail(1L, true) } returns
            Result.success(detail(imageUrl = "new.jpg", imageWidth = 0, imageHeight = 0))
        coEvery { feedLocal.updateNoteItem(1L, capture(json)) } returns Unit

        repository.updateNote(1L, "标题", "正文", listOf("new.jpg"), null, 1)

        val updated = gson.fromJson(json.captured, FeedItem::class.java)
        assertEquals("new.jpg", updated.coverImage)
        assertEquals(100, updated.coverWidth)
        assertEquals(200, updated.coverHeight)
    }

    @Test
    fun 视频笔记封面取视频字段而不是首图() = runBlocking {
        cacheOldFeedItem(oldFeedItem())
        val json = slot<String>()
        coEvery { remote.updateNote(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { remote.getNoteDetail(1L, true) } returns
            Result.success(detail(imageUrl = "first.jpg", noteType = 2))
        coEvery { feedLocal.updateNoteItem(1L, capture(json)) } returns Unit

        repository.updateNote(1L, "标题", "正文", listOf("first.jpg"), null, 1)

        // noteType 为 2 时走 video?.coverUrl 分支，video 为空所以封面为空
        assertNull(gson.fromJson(json.captured, FeedItem::class.java).coverImage)
    }

    @Test
    fun 编辑失败时不刷新快照() = runBlocking {
        coEvery { remote.updateNote(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("标题过长"))

        val result = repository.updateNote(1L, "标题", "正文", listOf("new.jpg"), null, 1)

        assertEquals("标题过长", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { feedLocal.updateNoteItem(any(), any()) }
        coVerify(exactly = 0) { remote.getNoteDetail(any(), any()) }
    }

    @Test
    fun 信息流里没有这条笔记时不动快照() = runBlocking {
        coEvery { feedLocal.getNoteItems(1L) } returns emptyList()
        coEvery { remote.updateNote(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { remote.getNoteDetail(1L, true) } returns Result.success(detail(imageUrl = "new.jpg"))

        repository.updateNote(1L, "标题", "正文", listOf("new.jpg"), null, 1)

        coVerify(exactly = 0) { feedLocal.updateNoteItem(any(), any()) }
    }
}
