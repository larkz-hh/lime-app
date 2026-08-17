package xyz.larkzhh.lime.ui.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import xyz.larkzhh.lime.navigation.AuthorProfileSession
import xyz.larkzhh.lime.navigation.AuthorProfileStore
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileNotesViewModel
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileViewModel
import javax.inject.Inject

/**
 * 笔记作者主页跨返回栈会话的宿主 ViewModel
 * 用于笔记详情页与作者主页共享和管理状态
 */
@HiltViewModel
class AuthorSessionHost @Inject constructor(
    private val userRepository: UserRepository,
    private val apiService: ApiService,
    private val noteRepository: NoteRepository,
    private val noteEventBus: NoteEventBus,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val childStore = ViewModelStore()// ViewModel存储容器
    private var authorId: Long? = null
    private var session: AuthorProfileSession? = null

    fun ensure(authorId: Long): AuthorProfileSession {
        session?.let { if (this.authorId == authorId) return it }
        clear()
        this.authorId = authorId
        val factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("userId" to authorId)),
                    userRepository = userRepository,
                    apiService = apiService,
                    context = appContext,
                )
            }
            initializer {
                ProfileNotesViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("userId" to authorId)),
                    noteRepository = noteRepository,
                    userRepository = userRepository,
                    eventBus = noteEventBus,
                )
            }
        }
        val provider = ViewModelProvider(childStore, factory)
        return AuthorProfileSession(
            profileViewModel = provider[ProfileViewModel::class.java],
            notesViewModel = provider[ProfileNotesViewModel::class.java],
        ).also {
            session = it
            AuthorProfileStore.put(authorId, it)
        }
    }

    private fun clear() {
        authorId?.let { AuthorProfileStore.remove(it) }
        childStore.clear()
        authorId = null
        session = null
    }

    override fun onCleared() {
        clear()
    }
}
