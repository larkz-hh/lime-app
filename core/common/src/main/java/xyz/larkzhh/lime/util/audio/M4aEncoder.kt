package xyz.larkzhh.lime.util.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PCM 转 AAC(m4a) 编码
 * 语音消息与音语识别共用 PCM，录音结束后压成 m4a 上传
 */
object M4aEncoder {

    private const val TIMEOUT_US = 10_000L// 单次 dequeue 超时
    private const val MAX_INPUT_SIZE = 16 * 1024// 编码区单次输入缓冲最大容量
    private const val BYTES_PER_SAMPLE = 2// 量化位数
    private const val DEFAULT_BIT_RATE = 64_000// AAC 编码目标比特率
    private const val STALL_TIMEOUT_MS = 10_000L// 卡死超时
    private const val BASE_BUDGET_MS = 30_000L// 总耗时

    /// 编码
    suspend fun encode(
        pcm: ByteArray,
        sampleRate: Int,
        channelCount: Int,
        outputFile: File,
        bitRate: Int = DEFAULT_BIT_RATE,
    ): Boolean = withContext(Dispatchers.IO) {
        if (pcm.isEmpty()) return@withContext false
        runCatching {
            encodeSync(pcm, sampleRate, channelCount, outputFile, bitRate)
        }.getOrDefault(false)
    }

    private fun encodeSync(
        pcm: ByteArray,
        sampleRate: Int,
        channelCount: Int,
        outputFile: File,
        bitRate: Int,
    ): Boolean {
        // 创建音频格式对象，AAC 编码
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount,
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE)
        }// 设置编码档次 AAC-LC、比特率、最大输入缓冲区大小

        // 创建编码器实例
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        var muxer: MediaMuxer? = null
        var trackIndex = -1// 音频轨道索引
        var muxerStarted = false
        var offset = 0
        var inputDone = false
        val bufferInfo = MediaCodec.BufferInfo()// 缓冲区信息对象
        val bytesPerSecond = sampleRate * channelCount * BYTES_PER_SAMPLE// 每秒音频字节数
        val audioDurationMs = pcm.size.toLong() * 1000 / bytesPerSecond// pcm 音频时长
        val deadline = System.currentTimeMillis() + BASE_BUDGET_MS + audioDurationMs * 2// 总耗时上限
        var lastProgressAt = System.currentTimeMillis()
        var completed = false
        outputFile.parentFile?.mkdirs()// 创建父目录
        outputFile.delete()

        try {
            // 启动编码器
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            while (true) {
                val now = System.currentTimeMillis()
                if (now > deadline || now - lastProgressAt > STALL_TIMEOUT_MS) break
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)// 申请输入缓冲区
                    if (inputIndex >= 0) {
                        // 获取缓冲区并清空
                        val input = codec.getInputBuffer(inputIndex)
                        if (input != null) {
                            input.clear()
                            // 写入的字节数
                            val size = minOf(input.remaining(), pcm.size - offset, MAX_INPUT_SIZE)
                            if (size > 0) input.put(pcm, offset, size)// 写入 PCM 数据
                            val ptsUs = offset * 1_000_000L / bytesPerSecond// 计算 PTS
                            // 判断是否是最后一批
                            if (offset + size >= pcm.size) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    size,
                                    ptsUs,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,// 结束标记
                                )
                                offset = pcm.size
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, size, ptsUs, 0)
                                offset += size
                            }
                            lastProgressAt = now
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, 0)
                        }
                    }
                }

                // 获取输出缓冲区
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    // 输出格式变更
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxer == null) {
                            muxer = MediaMuxer(
                                outputFile.absolutePath,
                                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                            )
                            trackIndex = muxer.addTrack(codec.outputFormat)// 添加音频轨道
                            muxer.start()
                            muxerStarted = true
                        }
                        lastProgressAt = now
                    }

                    // 正常输出帧
                    outputIndex >= 0 -> {
                        val output = codec.getOutputBuffer(outputIndex)// 获取输出缓冲区引用
                        val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                        if (output != null && bufferInfo.size > 0 && !isConfig && muxerStarted) {
                            // 缓冲区范围设置
                            output.position(bufferInfo.offset)
                            output.limit(bufferInfo.offset + bufferInfo.size)
                            muxer?.writeSampleData(trackIndex, output, bufferInfo)// 写入文件
                        }
                        codec.releaseOutputBuffer(outputIndex, false)// 归还缓冲区
                        lastProgressAt = now
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            completed = true
                            break
                        }
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            muxer?.let { muxerInstance ->
                if (muxerStarted) runCatching { muxerInstance.stop() }
                runCatching { muxerInstance.release() }
            }
        }
        return completed && outputFile.exists() && outputFile.length() > 0
    }
}
