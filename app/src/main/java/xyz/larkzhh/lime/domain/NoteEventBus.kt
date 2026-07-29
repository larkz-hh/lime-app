package xyz.larkzhh.lime.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class NoteEvent {
    data class LikeChanged(val noteId: Long, val liked: Boolean, val likeCount: Int) : NoteEvent()
    data class FavoriteChanged(val noteId: Long, val favorited: Boolean, val favCount: Int) : NoteEvent()
}

@Singleton
class NoteEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<NoteEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    suspend fun emit(event: NoteEvent) = _events.emit(event)
}
