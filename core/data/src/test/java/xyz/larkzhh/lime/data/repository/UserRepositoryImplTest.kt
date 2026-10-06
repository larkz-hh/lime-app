package xyz.larkzhh.lime.data.repository

import android.content.Context
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xyz.larkzhh.lime.data.im.ImManager
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.data.network.user.UserApi

/// 用户仓库
@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private val api = mockk<UserApi>()
    private val imManager = mockk<ImManager>(relaxed = true)
    private val tokenStorage = mockk<TokenStorage>(relaxed = true)
    private val mmkv = mockk<MMKV>(relaxed = true)
    private val gson = Gson()

    private val selfKey = "cached_user_data_5"
    private val fallbackKey = "cached_user_data"

    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { tokenStorage.currentUserId } returns 5L
        every { tokenStorage.currentUserIdFlow } returns MutableStateFlow(5L)
        every { mmkv.decodeString(selfKey) } returns null
        every { mmkv.decodeString(fallbackKey) } returns null
        repository = UserRepositoryImpl(api, mockk<Context>(relaxed = true), imManager, tokenStorage, mmkv, gson)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun user(id: Long = 5L, nickname: String = "小明") = UserData(
        id = id,
        nickname = nickname,
        handle = "handle$id",
        bio = null,
        avatar = null,
        backgroundImage = null,
        gender = null,
        birthday = null,
        region = null,
        role = "user",
    )

    @Test
    fun 更新用户同时写内存和当前账号的磁盘缓存() {
        val user = user()

        repository.updateUser(user)

        assertEquals(user, repository.userFlow.value)
        verify { mmkv.encode(selfKey, any<String>()) }
    }

    @Test
    fun 未登录时缓存键不带账号后缀() {
        every { tokenStorage.currentUserId } returns null

        repository.updateUser(user())

        verify { mmkv.encode(fallbackKey, any<String>()) }
    }

    @Test
    fun 清除用户同时清内存和磁盘() {
        repository.updateUser(user())

        repository.clearUser()

        assertNull(repository.userFlow.value)
        verify { mmkv.removeValueForKey(selfKey) }
    }

    @Test
    fun 清除登录态只清内存不删磁盘缓存() {
        repository.updateUser(user())

        repository.clearActiveSession()

        assertNull(repository.userFlow.value)
        verify(exactly = 0) { mmkv.removeValueForKey(any()) }
    }

    @Test
    fun 刷新失败时不覆盖本地缓存() = runBlocking {
        coEvery { api.getMe() } returns ApiResponse(500, "服务异常", null)

        val result = repository.refreshUser()

        assertEquals("服务异常", result.exceptionOrNull()?.message)
        verify(exactly = 0) { mmkv.encode(any(), any<String>()) }
    }

    @Test
    fun 刷新成功同时写入内存与磁盘() = runBlocking {
        val fresh = user(nickname = "新名字")
        coEvery { api.getMe() } returns ApiResponse(200, "ok", fresh)

        val result = repository.refreshUser()

        assertEquals(fresh, result.getOrThrow())
        assertEquals(fresh, repository.userFlow.value)
        verify { mmkv.encode(selfKey, any<String>()) }
    }

    @Test
    fun 按id查询成功后进入内存缓存() = runBlocking {
        val other = user(id = 9L)
        coEvery { api.getUserById(9L) } returns ApiResponse(200, "ok", other)

        repository.getUserById(9L)

        assertEquals(other, repository.getCachedUserById(9L))
    }

    @Test
    fun 按id查询失败不写入缓存() = runBlocking {
        coEvery { api.getUserById(9L) } returns ApiResponse(404, "用户不存在", null)

        val result = repository.getUserById(9L)

        assertTrue(result.isFailure)
        assertNull(repository.getCachedUserById(9L))
    }

    @Test
    fun 按handle查询用返回体的id做缓存键() = runBlocking {
        val other = user(id = 33L)
        coEvery { api.getUserByHandle("abc") } returns ApiResponse(200, "ok", other)

        repository.getUserByHandle("abc")

        assertEquals(other, repository.getCachedUserById(33L))
    }

    @Test
    fun IM未登录时不推送资料变更() = runBlocking {
        every { imManager.getLoginUser() } returns null
        coEvery { api.updateMe(any()) } returns ApiResponse(200, "ok", user())

        repository.updateProfile("小明", null, 0, null, null)

        coVerify(exactly = 0) { imManager.updateSelfProfile(any(), any()) }
    }

    @Test
    fun IM已登录时推送资料变更() = runBlocking {
        every { imManager.getLoginUser() } returns "im-user"
        coEvery { api.updateMe(any()) } returns ApiResponse(200, "ok", user())

        repository.updateProfile("小明", null, 0, null, null)

        coVerify(exactly = 1) { imManager.updateSelfProfile("小明", any()) }
    }

    @Test
    fun IM推送失败不影响资料更新结果() = runBlocking {
        every { imManager.getLoginUser() } returns "im-user"
        coEvery { api.updateMe(any()) } returns ApiResponse(200, "ok", user())
        coEvery { imManager.updateSelfProfile(any(), any()) } throws RuntimeException("IM 断线")

        val result = repository.updateProfile("小明", null, 0, null, null)

        assertTrue(result.isSuccess)
    }
}
