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
}
