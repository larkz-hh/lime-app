package xyz.larkzhh.lime.data.repository

import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户数据单一数据源。
 * 内存通过 StateFlow 与 MMKV 缓存，页面通过观察 userFlow 同步最新数据。
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : UserRepository {

    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val gson = Gson()

    private val _userFlow = MutableStateFlow<UserData?>(loadFromCache())
    override val userFlow: StateFlow<UserData?> = _userFlow.asStateFlow()

    /// 加载缓存
    private fun loadFromCache(): UserData? {
        val json = mmkv.decodeString(KEY_USER) ?: return null
        return runCatching { gson.fromJson(json, UserData::class.java) }.getOrNull()
    }

    /// 更新用户数据
    override fun updateUser(user: UserData) {
        _userFlow.value = user
        mmkv.encode(KEY_USER, gson.toJson(user))
    }

    /// 网络刷新
    override suspend fun refreshUser(): Result<UserData> = runCatching {
        val response = apiService.getMe()
        check(response.code == 200 && response.data != null) { response.message }
        updateUser(response.data)
        response.data
    }

    /// 清空用户数据
    override fun clearUser() {
        _userFlow.value = null
        mmkv.removeValueForKey(KEY_USER)
    }

    private companion object {
        const val KEY_USER = "cached_user_data"
    }
}
