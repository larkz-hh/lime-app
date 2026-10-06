package xyz.larkzhh.lime.data.repository

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.local.note.NoteCacheLocalDataSource
import xyz.larkzhh.lime.data.network.comment.CommentRemoteDataSource
import java.io.File

/// 评论仓库的语音上传组装
class CommentRepositoryImplTest {

    private val remote = mockk<CommentRemoteDataSource>(relaxed = true)
    private val noteCache = mockk<NoteCacheLocalDataSource>(relaxed = true)
    private val repository = CommentRepositoryImpl(remote, noteCache, mockk<Context>(relaxed = true))

    @Test
    fun 语音文件读不出来时直接失败且不调接口() = runBlocking {
        val missing = File("no-such-file-${System.nanoTime()}.m4a")

        val result = repository.uploadCommentVoice(missing)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { remote.uploadCommentVoice(any()) }
    }

    @Test
    fun 语音文件可读时按原文件名组装并交给远端() = runBlocking {
        val file = File.createTempFile("voice", ".m4a").apply { writeBytes(ByteArray(16)) }
        try {
            val result = repository.uploadCommentVoice(file)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { remote.uploadCommentVoice(any()) }
        } finally {
            file.delete()
        }
    }

    @Test
    fun 删除评论失败时返回远端失败() = runBlocking {
        coEvery { remote.deleteComment(1L) } returns Result.failure(RuntimeException("无权删除"))

        val result = repository.deleteComment(1L)

        assertEquals("无权删除", result.exceptionOrNull()?.message)
    }
}
