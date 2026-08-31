package xyz.larkzhh.lime.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Video : Screen("video")
    object Search : Screen("search?query={query}") {
        const val ROUTE = "search?query={query}"
        const val ARG_QUERY = "query"
        const val BASE_ROUTE = "search"
        fun createRoute(query: String) =
            if (query.isBlank()) BASE_ROUTE else "search?query=${Uri.encode(query)}"
    }
    object Publish : Screen("publish")
    object Message : Screen("message")
    object Profile : Screen("profile")
    object UserProfile : Screen("user_profile/{userId}") {
        const val ROUTE = "user_profile/{userId}"
        fun createRoute(userId: Long) = "user_profile/$userId"
    }
    object Detail : Screen("detail/{noteId}") {
        const val ROUTE = "detail/{noteId}"
        fun createRoute(noteId: String) = "detail/$noteId"
    }
    object VideoFeed : Screen("video_feed/{noteId}?source={source}") {
        const val ROUTE = "video_feed/{noteId}?source={source}"
        const val SOURCE_RECOMMENDATION = "recomment"// 推荐
        const val SOURCE_PERSONAL = "personal"// 个人列表
        const val SOURCE_TAB = "tab"// 底部栏视频
        fun createRoute(noteId: Long, source: String) = "video_feed/$noteId?source=$source"
    }
    object EditProfile : Screen("edit_profile")
    object QrScan : Screen("qr_scan")
    object PhotoPicker : Screen("photo_picker")
    object NotePublish : Screen("note_publish")
    object VideoPublish : Screen("video_publish")
    object CoverPicker : Screen("cover_picker")
    object BrowseHistory : Screen("browse_history")
    object CommentPhotoPicker : Screen("comment_photo_picker")
    object AiChat : Screen("ai_chat/{conversationId}") {
        const val ROUTE = "ai_chat/{conversationId}"
        const val NEW_CONVERSATION = "new"
        const val LATEST_CONVERSATION = "latest"
        fun createRoute(conversationId: String) = "ai_chat/$conversationId"
    }
}
