package xyz.larkzhh.lime.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import xyz.larkzhh.lime.util.text.stripMarkdown


object TtsManager {

    private var tts: TextToSpeech? = null
    private var currentUtterance: String? = null

    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    // 朗读
    fun speak(context: Context, messageId: Long, text: String) {
        val content = text.stripMarkdown()
        if (content.isEmpty()) return

        val engine = tts
        if (engine == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    configure(tts)
                    startSpeaking(messageId, content)
                }
            }
        } else {
            startSpeaking(messageId, content)
        }
    }

    /// 停止朗读
    fun stop() {
        tts?.stop()
        _speakingMessageId.value = null
        currentUtterance = null
    }

    // 中断朗读
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _speakingMessageId.value = null
        currentUtterance = null
    }

    private fun configure(engine: TextToSpeech?) {
        engine?.language = Locale.CHINESE
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                finishUtterance(utteranceId)
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                finishUtterance(utteranceId)
            }

//            override fun onError(utteranceId: String?, errorCode: Int) {
//                finishUtterance(utteranceId)
//            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                finishUtterance(utteranceId)
            }
        })
    }

    private fun startSpeaking(messageId: Long, content: String) {
        val utterance = "lime-tts-$messageId"
        currentUtterance = utterance
        _speakingMessageId.value = messageId
        tts?.speak(content, TextToSpeech.QUEUE_FLUSH, null, utterance)
    }

    private fun finishUtterance(utteranceId: String?) {
        if (utteranceId == currentUtterance) {
            currentUtterance = null
            _speakingMessageId.value = null
        }
    }
}
