package xyz.larkzhh.lime.data.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.larkzhh.lime.data.network.model.UnreadCountData
import xyz.larkzhh.lime.domain.ForceLogoutBus
import kotlin.time.Duration.Companion.milliseconds

private const val RECONNECT_DELAY_MS = 5_000L

/**
 * 站内通知未读数 SSE 订阅
 */
fun notificationUnreadFlow(
    client: OkHttpClient,
    url: String,
    gson: Gson,
): Flow<UnreadCountData> = flow {
    var eventName: String? = null
    while (true) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()
        try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    delay(RECONNECT_DELAY_MS.milliseconds)
                    return@use
                }
                val source = resp.body?.source() ?: run {
                    delay(RECONNECT_DELAY_MS.milliseconds)
                    return@use
                }
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("event:")) {
                        eventName = line.removePrefix("event:").trim()
                        continue
                    }
                    if (line.startsWith("data:")) {
                        val payload = line.removePrefix("data:").trim()
                        if (payload.isEmpty()) continue
                        if (eventName == "kick") {
                            // 后端 kick 事件，异地登陆强制下线，改密码静默
                            val reason = runCatching {
                                JsonParser.parseString(payload).asJsonObject.get("reason")?.asString
                            }.getOrNull()
                            if (reason != "password_changed") {
                                ForceLogoutBus.emit()
                            }
                            return@flow
                        }
                        val data = runCatching {
                            gson.fromJson(payload, UnreadCountData::class.java)
                        }.getOrNull() ?: continue
                        emit(data)
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) { }
        delay(RECONNECT_DELAY_MS.milliseconds)
    }
}.flowOn(Dispatchers.IO)
