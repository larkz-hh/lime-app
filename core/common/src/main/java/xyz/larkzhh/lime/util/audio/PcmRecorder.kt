package xyz.larkzhh.lime.util.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/**
 * PCM 录音器
 */
class PcmRecorder(
    val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    val channelCount: Int = 1,
) {

    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private val buffer = ByteArrayOutputStream()
    private val lock = Any()

    @Volatile
    private var running = false

    /// 最近一段的振幅
    @Volatile
    var amplitude: Float = 0f
        private set

    /// 每段 PCM 回调
    @Volatile
    var onChunk: ((ByteArray) -> Unit)? = null

    /// 已录制字节数
    val recordedBytes: Int get() = synchronized(lock) { buffer.size() }

    /// 已录制秒数
    val recordedSeconds: Int
        get() = recordedBytes / (sampleRate * channelCount * BYTES_PER_SAMPLE)

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        // 获取最小缓冲区大小
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return false
        val bufferSize = maxOf(minBuffer * 2, sampleRate / 5 * channelCount * BYTES_PER_SAMPLE)
        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
        } catch (_: Exception) {
            null
        } ?: return false
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }
        buffer.reset()
        amplitude = 0f
        record = recorder
        running = true
        try {
            recorder.startRecording()
        } catch (_: Exception) {
            running = false
            recorder.release()
            record = null
            return false
        }
        thread = Thread({ loop(recorder, bufferSize) }, "lime-pcm-recorder").apply { start() }
        return true
    }

    /// 停止并返回整段 PCM
    fun stop(): ByteArray {
        running = false
        val recorder = record
        record = null
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        thread?.let { runCatching { it.join(THREAD_JOIN_TIMEOUT_MS) } }
        thread = null
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        amplitude = 0f
        onChunk = null
        return synchronized(lock) { buffer.toByteArray() }
    }

    private fun loop(recorder: AudioRecord, bufferSize: Int) {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }// 提升优先级
        val chunk = ByteArray(bufferSize)
        while (running) {
            val read = try {
                recorder.read(chunk, 0, chunk.size)
            } catch (_: Exception) {
                -1
            }
            if (read <= 0) {
                if (read < 0) break else continue
            }
            val data = chunk.copyOf(read)
            synchronized(lock) { buffer.write(data) }
            amplitude = rms(data)
            onChunk?.invoke(data)
        }
    }

    /// 归一化振幅
    private fun rms(data: ByteArray): Float {
        if (data.size < 2) return 0f
        var sum = 0.0
        var index = 0
        while (index + 1 < data.size) {
            val sample = ((data[index + 1].toInt() shl 8) or (data[index].toInt() and 0xFF)).toShort().toInt()
            sum += sample.toDouble() * sample
            index += 2
        }
        val value = sqrt(sum / (data.size / 2))
        return (value / 6000.0).coerceIn(0.0, 1.0).toFloat()
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 16_000
        private const val BYTES_PER_SAMPLE = 2
        /// 停止时等待采集线程退出的上限
        private const val THREAD_JOIN_TIMEOUT_MS = 200L
    }
}
