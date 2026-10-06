package xyz.larkzhh.lime.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xyz.larkzhh.lime.data.local.notification.NotificationLocalDataSource
import xyz.larkzhh.lime.data.network.model.NotificationData
import xyz.larkzhh.lime.data.network.model.NotificationListResponse
import xyz.larkzhh.lime.data.network.model.UnreadCountData
import xyz.larkzhh.lime.data.network.notification.NotificationRemoteDataSource
import xyz.larkzhh.lime.domain.model.NotificationCategory

/// 站内通知仓库
class NotificationRepositoryImplTest {

    private val remote = mockk<NotificationRemoteDataSource>(relaxed = true)
    private val local = mockk<NotificationLocalDataSource>(relaxed = true)
    private val repository = NotificationRepositoryImpl(remote, local)

    private val item = NotificationData(id = 1L, type = 1)

    @Before
    fun setUp() {
        every { local.observeAll() } returns MutableStateFlow(emptyList())
        coEvery { remote.getUnreadCount() } returns Result.success(UnreadCountData())
    }

    @Test
    fun 分页拉取成功后写入本地并返回数据() = runBlocking {
        val response = NotificationListResponse(items = listOf(item), nextCursor = "c1", hasMore = true)
        coEvery { remote.getNotifications(any(), any(), any()) } returns Result.success(response)

        val result = repository.loadPage(cursor = null, size = 20, type = null)

        assertEquals(response, result.getOrThrow())
        coVerify(exactly = 1) { local.upsert(listOf(item)) }
    }

    @Test
    fun 分页拉取失败时不写本地() = runBlocking {
        coEvery { remote.getNotifications(any(), any(), any()) } returns Result.failure(RuntimeException("断网了"))

        val result = repository.loadPage(cursor = null, size = 20, type = null)

        assertEquals("断网了", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { local.upsert(any()) }
    }

    @Test
    fun 标记单条已读同时写本地和远端() = runBlocking {
        coEvery { remote.markRead(1L) } returns Result.success(Unit)

        val result = repository.markRead(1L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { local.markRead(1L) }
        coVerify(exactly = 1) { remote.markRead(1L) }
    }

    @Test
    fun 标记单条已读远端失败时仍返回失败() = runBlocking {
        coEvery { remote.markRead(1L) } returns Result.failure(RuntimeException("服务异常"))

        val result = repository.markRead(1L)

        assertEquals("服务异常", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { local.markRead(1L) }
    }

    @Test
    fun 全部已读时清空本地并透传null给远端() = runBlocking {
        val result = repository.markAllRead(category = null)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { local.markAllRead() }
        coVerify(exactly = 1) { remote.markAllRead(type = null) }
    }

    @Test
    fun 按分类已读时用类型码拼接参数() = runBlocking {
        val result = repository.markAllRead(NotificationCategory.Follows)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { local.markAllReadByTypes(listOf(5)) }
        coVerify(exactly = 1) { remote.markAllRead(type = "5") }
    }

    @Test
    fun 赞和收藏分类使用三个类型码() = runBlocking {
        repository.markAllRead(NotificationCategory.LikesFavorites)

        coVerify(exactly = 1) { local.markAllReadByTypes(listOf(1, 2, 6)) }
        coVerify(exactly = 1) { remote.markAllRead(type = "1,2,6") }
    }

    @Test
    fun 全部已读即使远端失败也返回成功() = runBlocking {
        coEvery { remote.markAllRead(any()) } returns Result.failure(RuntimeException("服务异常"))

        assertTrue(repository.markAllRead(category = null).isSuccess)
    }

    @Test
    fun 删除失败时返回远端的失败() = runBlocking {
        coEvery { remote.delete(1L) } returns Result.failure(RuntimeException("不存在"))

        val result = repository.delete(1L)

        assertEquals("不存在", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { local.delete(1L) }
    }

    @Test
    fun 本地删除抛异常也不影响继续调远端() = runBlocking {
        coEvery { local.delete(1L) } throws RuntimeException("数据库锁住了")
        coEvery { remote.delete(1L) } returns Result.success(Unit)

        val result = repository.delete(1L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { remote.delete(1L) }
    }

    @Test
    fun 清空全部失败时返回远端的失败() = runBlocking {
        coEvery { remote.clearAll() } returns Result.failure(RuntimeException("服务异常"))

        val result = repository.clearAll()

        assertEquals("服务异常", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { local.clearAll() }
    }

    @Test
    fun 拉取未读数成功时更新总未读() = runBlocking {
        coEvery { remote.getUnreadCount() } returns Result.success(UnreadCountData(total = 7))

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        coVerify(atLeast = 1) { remote.getUnreadCount() }
    }
}
