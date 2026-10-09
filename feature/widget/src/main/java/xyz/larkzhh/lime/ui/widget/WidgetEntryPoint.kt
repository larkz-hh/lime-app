package xyz.larkzhh.lime.ui.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.larkzhh.lime.domain.repository.SearchRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun searchRepository(): SearchRepository
}
