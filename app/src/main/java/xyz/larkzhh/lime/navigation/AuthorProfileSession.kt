package xyz.larkzhh.lime.navigation

import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileNotesViewModel
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileViewModel

/**
 * 笔记作者主页的跨返回栈状态会话。
 * 保留用户页状态
 * 由详情页按作者 id 建立，从评论进入的用户主页不取该会话
 */
class AuthorProfileSession(
    val profileViewModel: ProfileViewModel,
    val notesViewModel: ProfileNotesViewModel,
) {
    var currentPage: Int = 0// 当前tab下标
    var headerOffsetPx: Float = 0f// header折叠偏移量
    val tabScroll: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()// 滚动位置
    var backgroundDominantRgb: Int? = null
    // 头部、Tab栏、顶栏的测量高度
    var headerHeightPx: Int = 0
    var tabBarHeightPx: Int = 0
    var topBarHeightPx: Int = 0
}

/// 作者主页会话的进程内存储
object AuthorProfileStore {
    private val sessions = mutableMapOf<Long, AuthorProfileSession>()

    fun put(authorId: Long, session: AuthorProfileSession) {
        sessions[authorId] = session
    }

    fun get(authorId: Long): AuthorProfileSession? = sessions[authorId]

    fun remove(authorId: Long) {
        sessions.remove(authorId)
    }
}
