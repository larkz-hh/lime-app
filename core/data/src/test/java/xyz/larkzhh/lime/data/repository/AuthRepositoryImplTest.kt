package xyz.larkzhh.lime.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.network.auth.AuthApi
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.TokenData

/// 认证仓库
class AuthRepositoryImplTest {

    private val api = mockk<AuthApi>()
    private val tokenStorage = mockk<TokenStorage>(relaxed = true)
    private val repository = AuthRepositoryImpl(api, tokenStorage)

    private val token = TokenData(accessToken = "access-1", refreshToken = "refresh-1", expiresIn = 3600L)

    @Test
    fun 登录成功后保存令牌() = runBlocking {
        coEvery { api.login(any()) } returns ApiResponse(200, "ok", token)

        val result = repository.login("a@b.com", "pw", null)

        assertEquals(token, result.getOrThrow())
        verify { tokenStorage.saveTokens("access-1", "refresh-1", 3600L) }
    }

    @Test
    fun 登录非200时失败且不保存令牌() = runBlocking {
        coEvery { api.login(any()) } returns ApiResponse(401, "密码错误", null)

        val result = repository.login("a@b.com", "pw", null)

        assertEquals("密码错误", result.exceptionOrNull()?.message)
        verify(exactly = 0) { tokenStorage.saveTokens(any(), any(), any()) }
    }

    @Test
    fun 本地没有刷新令牌时不调用接口直接失败() = runBlocking {
        every { tokenStorage.refreshToken } returns null

        val result = repository.refreshToken()

        assertTrue(result.isFailure)
        assertEquals("未登录", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { api.refreshToken(any()) }
    }

    @Test
    fun 刷新成功后覆盖本地令牌且不清空() = runBlocking {
        every { tokenStorage.refreshToken } returns "refresh-1"
        coEvery { api.refreshToken(any()) } returns ApiResponse(200, "ok", token)

        val result = repository.refreshToken()

        assertEquals(token, result.getOrThrow())
        verify { tokenStorage.saveTokens("access-1", "refresh-1", 3600L) }
        verify(exactly = 0) { tokenStorage.clearTokens() }
    }

    @Test
    fun 刷新失败先清本地令牌再抛错() = runBlocking {
        every { tokenStorage.refreshToken } returns "refresh-1"
        coEvery { api.refreshToken(any()) } returns ApiResponse(401, "令牌已失效", null)

        val result = repository.refreshToken()

        assertTrue(result.isFailure)
        assertEquals("令牌已失效", result.exceptionOrNull()?.message)
        verify(exactly = 1) { tokenStorage.clearTokens() }
        verify(exactly = 0) { tokenStorage.saveTokens(any(), any(), any()) }
    }

    @Test
    fun 登出时接口抛异常也要清本地令牌() = runBlocking {
        coEvery { api.logout() } throws RuntimeException("网络断了")

        repository.logout()

        verify(exactly = 1) { tokenStorage.clearTokens() }
    }

    @Test
    fun 改密成功并返回新令牌时保存() = runBlocking {
        coEvery { api.changePassword(any()) } returns ApiResponse(200, "ok", token)

        val result = repository.changePassword("old", null, "new")

        assertTrue(result.isSuccess)
        verify { tokenStorage.saveTokens("access-1", "refresh-1", 3600L) }
        verify(exactly = 0) { tokenStorage.clearTokens() }
    }

    @Test
    fun 改密成功但没有新令牌时清空本地() = runBlocking {
        coEvery { api.changePassword(any()) } returns ApiResponse(200, "ok", null)

        val result = repository.changePassword("old", null, "new")

        assertTrue(result.isSuccess)
        verify(exactly = 1) { tokenStorage.clearTokens() }
        verify(exactly = 0) { tokenStorage.saveTokens(any(), any(), any()) }
    }

    @Test
    fun 改密失败不碰本地令牌() = runBlocking {
        coEvery { api.changePassword(any()) } returns ApiResponse(400, "原密码错误", null)

        val result = repository.changePassword("old", null, "new")

        assertFalse(result.isSuccess)
        verify(exactly = 0) { tokenStorage.clearTokens() }
        verify(exactly = 0) { tokenStorage.saveTokens(any(), any(), any()) }
    }

    @Test
    fun 登录状态委托给本地存储() {
        every { tokenStorage.isLoggedIn() } returns true
        assertTrue(repository.isLoggedIn())

        every { tokenStorage.isLoggedIn() } returns false
        assertFalse(repository.isLoggedIn())
    }
}
