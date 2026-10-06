package xyz.larkzhh.lime.util.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络状态监听器
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isUnmetered = MutableStateFlow(true)
    val isUnmetered: StateFlow<Boolean> = _isUnmetered.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            refreshUnmetered()
        }

        override fun onLost(network: Network) {
            _isOnline.value = hasAnyConnection()
            refreshUnmetered()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            _isUnmetered.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }
    }

    init {
        _isOnline.value = hasAnyConnection()
        refreshUnmetered()
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)// 注册回调
        }
    }

    /// 获取当前网络能力
    private fun currentCapabilities(): NetworkCapabilities? = runCatching {
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    }.getOrNull()

    /// 判断是否有网络连接
    private fun hasAnyConnection(): Boolean =
        currentCapabilities()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    /// 判断当前网络是否计费
    private fun refreshUnmetered() {
        _isUnmetered.value =
            currentCapabilities()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
    }
}
