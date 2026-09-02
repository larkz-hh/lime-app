package xyz.larkzhh.lime.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.larkzhh.lime.data.local.chat.ChatDao
import xyz.larkzhh.lime.data.local.chat.ChatDatabase
import xyz.larkzhh.lime.data.local.feed.FeedDao
import xyz.larkzhh.lime.data.local.feed.FeedDatabase
import xyz.larkzhh.lime.data.local.note.NoteCacheDao
import xyz.larkzhh.lime.data.local.note.NoteCacheDatabase
import javax.inject.Singleton

/**
 * Room 数据库 DI 模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase =
        Room.databaseBuilder(context, ChatDatabase::class.java, "lime_chat.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideChatDao(database: ChatDatabase): ChatDao = database.chatDao()

    @Provides
    @Singleton
    fun provideFeedDatabase(@ApplicationContext context: Context): FeedDatabase =
        Room.databaseBuilder(context, FeedDatabase::class.java, "lime_feed.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideFeedDao(database: FeedDatabase): FeedDao = database.feedDao()

    @Provides
    @Singleton
    fun provideNoteCacheDatabase(@ApplicationContext context: Context): NoteCacheDatabase =
        Room.databaseBuilder(context, NoteCacheDatabase::class.java, "lime_note_cache.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideNoteCacheDao(database: NoteCacheDatabase): NoteCacheDao = database.noteCacheDao()
}
