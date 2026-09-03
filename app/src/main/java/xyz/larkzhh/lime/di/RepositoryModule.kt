package xyz.larkzhh.lime.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.larkzhh.lime.data.repository.AiRepositoryImpl
import xyz.larkzhh.lime.data.repository.AuthRepositoryImpl
import xyz.larkzhh.lime.data.repository.CommentRepositoryImpl
import xyz.larkzhh.lime.data.repository.DanmakuRepositoryImpl
import xyz.larkzhh.lime.data.repository.FollowRepositoryImpl
import xyz.larkzhh.lime.data.repository.NoteRepositoryImpl
import xyz.larkzhh.lime.data.repository.SearchRepositoryImpl
import xyz.larkzhh.lime.data.repository.UserRepositoryImpl
import xyz.larkzhh.lime.data.repository.chat.ChatRepositoryImpl
import xyz.larkzhh.lime.domain.repository.AiRepository
import xyz.larkzhh.lime.domain.repository.AuthRepository
import xyz.larkzhh.lime.domain.repository.ChatRepository
import xyz.larkzhh.lime.domain.repository.CommentRepository
import xyz.larkzhh.lime.domain.repository.DanmakuRepository
import xyz.larkzhh.lime.domain.repository.FollowRepository
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.domain.repository.SearchRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Singleton

/**
 * 仓库 DI 模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(impl: CommentRepositoryImpl): CommentRepository

    @Binds
    @Singleton
    abstract fun bindDanmakuRepository(impl: DanmakuRepositoryImpl): DanmakuRepository

    @Binds
    @Singleton
    abstract fun bindFollowRepository(impl: FollowRepositoryImpl): FollowRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
