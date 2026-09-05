package xyz.larkzhh.lime.data.im

import android.content.Context
import com.tencent.imsdk.v2.V2TIMAdvancedMsgListener
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMConversationListener
import com.tencent.imsdk.v2.V2TIMConversationResult
import com.tencent.imsdk.v2.V2TIMDownloadCallback
import com.tencent.imsdk.v2.V2TIMElem
import com.tencent.imsdk.v2.V2TIMImageElem
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSendCallback
import com.tencent.imsdk.v2.V2TIMUserFullInfo
import com.tencent.imsdk.v2.V2TIMValueCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 腾讯云 IM SDK 封装
 */
@Singleton
class ImManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var initialized = false
    private var sdkAppId = 0L

    // msgID → V2TIMMessage 缓存
    private val messageCache = ConcurrentHashMap<String, V2TIMMessage>()

    private val _newMessages = MutableSharedFlow<V2TIMMessage>(extraBufferCapacity = 64)
    // 收到的新单聊消息流
    val newMessages: SharedFlow<V2TIMMessage> = _newMessages.asSharedFlow()

    private val _revokedMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    // 对方撤回消息的 msgID 流
    val revokedMessages: SharedFlow<String> = _revokedMessages.asSharedFlow()

    private val _conversationChanges = MutableSharedFlow<List<V2TIMConversation>>(extraBufferCapacity = 16)
    // 会话列表变化流
    val conversationChanges: SharedFlow<List<V2TIMConversation>> = _conversationChanges.asSharedFlow()

    private val messageListener = object : V2TIMAdvancedMsgListener() {
        override fun onRecvNewMessage(msg: V2TIMMessage?) {
            msg?.let {
                it.msgID?.let { id -> messageCache[id] = it }
                _newMessages.tryEmit(it)
            }
        }

        override fun onRecvMessageRevoked(msgID: String?, info: V2TIMUserFullInfo?, reason: String?) {
            msgID?.let { _revokedMessages.tryEmit(it) }
        }
    }

    private val conversationListener = object : V2TIMConversationListener() {
        override fun onConversationChanged(conversationList: List<V2TIMConversation>?) {
            conversationList?.let { _conversationChanges.tryEmit(it) }
        }

        override fun onNewConversation(conversationList: List<V2TIMConversation>?) {
            conversationList?.let { _conversationChanges.tryEmit(it) }
        }
    }

    /// 初始化 SDK
    fun init(sdkAppId: Long) {
        if (initialized && this.sdkAppId == sdkAppId) return
        V2TIMManager.getInstance().initSDK(context, sdkAppId.toInt(), V2TIMSDKConfig())
        V2TIMManager.getMessageManager().addAdvancedMsgListener(messageListener)
        V2TIMManager.getConversationManager().addConversationListener(conversationListener)
        this.sdkAppId = sdkAppId
        this.initialized = true
    }

    /// 登录
    suspend fun login(userId: String, userSig: String): Unit = suspendCancellableCoroutine { cont ->
        V2TIMManager.getInstance().login(userId, userSig, object : V2TIMCallback {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "IM 登录失败"))
            }
        })
    }

    /// 登出
    suspend fun logout(): Unit = suspendCancellableCoroutine { cont ->
        V2TIMManager.getInstance().logout(object : V2TIMCallback {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (cont.isActive) cont.resume(Unit)
            }
        })
    }

    /// 当前已登录的 IM 用户 ID
    fun getLoginUser(): String? = V2TIMManager.getInstance().loginUser

    /// 同步昵称、头像到 IM 用户资料
    suspend fun updateSelfProfile(nickname: String, avatarUrl: String?): Unit =
        suspendCancellableCoroutine { cont ->
            val info = V2TIMUserFullInfo()
            info.setNickname(nickname)
            avatarUrl?.let { info.faceUrl = it }
            V2TIMManager.getInstance().setSelfInfo(info, object : V2TIMCallback {
                override fun onSuccess() {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onError(code: Int, desc: String?) {
                    if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "资料同步失败"))
                }
            })
        }


    /// 发送文本消息
    suspend fun sendTextMessage(conversationId: String, text: String): V2TIMMessage = sendMessage(
        V2TIMManager.getMessageManager().createTextMessage(text),
        conversationId,
    )

    /// 发送图片消息（
    suspend fun sendImageMessage(conversationId: String, imagePath: String): V2TIMMessage = sendMessage(
        V2TIMManager.getMessageManager().createImageMessage(imagePath),
        conversationId,
    )

    /// 发送消息通用实现
    private suspend fun sendMessage(message: V2TIMMessage, conversationId: String): V2TIMMessage =
        suspendCancellableCoroutine { cont ->
            val receiver = conversationId.removePrefix("c2c_")
            V2TIMManager.getMessageManager().sendMessage(
                message,
                receiver,
                null,
                V2TIMMessage.V2TIM_PRIORITY_DEFAULT,
                false,
                null,
                object : V2TIMSendCallback<V2TIMMessage> {
                    override fun onSuccess(t: V2TIMMessage?) {
                        t?.msgID?.let { id -> messageCache[id] = t }
                        if (cont.isActive) cont.resume(t ?: message)
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "发送失败"))
                    }

                    override fun onProgress(progress: Int) = Unit
                },
            )
        }

    /// 拉取单聊历史消息
    suspend fun getC2CHistoryMessageList(userId: String, count: Int): List<V2TIMMessage> =
        suspendCancellableCoroutine { cont ->
            V2TIMManager.getMessageManager().getC2CHistoryMessageList(
                userId,
                count,
                null,
                object : V2TIMValueCallback<List<V2TIMMessage>> {
                    override fun onSuccess(t: List<V2TIMMessage>?) {
                        t?.forEach { it.msgID?.let { id -> messageCache[id] = it } }
                        if (cont.isActive) cont.resume(t ?: emptyList())
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (cont.isActive) cont.resume(emptyList())
                    }
                },
            )
        }


    /// 撤回一条消息
    suspend fun revokeMessage(msgId: String): Unit = suspendCancellableCoroutine { cont ->
        val msg = messageCache[msgId]
        if (msg == null) {
            if (cont.isActive) cont.resumeWithException(ImException(-1, "消息不存在，无法撤回"))
            return@suspendCancellableCoroutine
        }
        V2TIMManager.getMessageManager().revokeMessage(msg, object : V2TIMCallback {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "撤回失败"))
            }
        })
    }

    /// 删除一条消息
    suspend fun deleteMessage(msgId: String): Unit = suspendCancellableCoroutine { cont ->
        val msg = messageCache[msgId]
        if (msg == null) {
            if (cont.isActive) cont.resumeWithException(ImException(-1, "消息不存在，无法删除"))
            return@suspendCancellableCoroutine
        }
        V2TIMManager.getMessageManager().deleteMessages(listOf(msg), object : V2TIMCallback {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "删除失败"))
            }
        })
    }

    /// 清空与某用户的单聊历史消息
    suspend fun clearC2CHistoryMessage(userId: String): Unit = suspendCancellableCoroutine { cont ->
        V2TIMManager.getMessageManager().clearC2CHistoryMessage(userId, object : V2TIMCallback {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "清空失败"))
            }
        })
    }

    /// 将单聊会话标记为已读
    suspend fun markC2CMessageAsRead(userId: String): Unit = suspendCancellableCoroutine { cont ->
        V2TIMManager.getMessageManager().markC2CMessageAsRead(userId, object : V2TIMCallback {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "标记已读失败"))
            }
        })
    }

    /// 把图片消息下载到应用缓存目录
    suspend fun downloadImageToCache(msgId: String): String? = suspendCancellableCoroutine { cont ->
        val msg = messageCache[msgId]
        val image = msg?.imageElem?.imageList
            ?.firstOrNull { it.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN }
            ?: msg?.imageElem?.imageList?.firstOrNull()
        if (image == null) {
            if (cont.isActive) cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val saveFile = File(context.cacheDir, "im_msg_$msgId.jpg")
        image.downloadImage(saveFile.absolutePath, object : V2TIMDownloadCallback {
            override fun onProgress(info: V2TIMElem.V2ProgressInfo?) = Unit

            override fun onSuccess() {
                if (cont.isActive) cont.resume(saveFile.absolutePath)
            }

            override fun onError(code: Int, desc: String?) {
                saveFile.delete()
                if (cont.isActive) cont.resume(null)
            }
        })
    }

    /// 拉取会话列表。按时间倒序
    suspend fun getConversationList(): List<V2TIMConversation> =
        suspendCancellableCoroutine { cont ->
            V2TIMManager.getConversationManager().getConversationList(
                0L,
                100,
                object : V2TIMValueCallback<V2TIMConversationResult> {
                    override fun onSuccess(t: V2TIMConversationResult?) {
                        if (cont.isActive) cont.resume(t?.conversationList ?: emptyList())
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (cont.isActive) cont.resume(emptyList())
                    }
                },
            )
        }

    /// 删除会话消息
    suspend fun deleteConversation(conversationId: String): Unit =
        suspendCancellableCoroutine { cont ->
            V2TIMManager.getConversationManager().deleteConversation(
                conversationId,
                object : V2TIMCallback {
                    override fun onSuccess() {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "删除会话失败"))
                    }
                },
            )
        }
}

/// IM 业务异常
class ImException(val code: Int, message: String) : Exception(message)
