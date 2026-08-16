package xyz.larkzhh.lime.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Video : Screen("video")
    object Search : Screen("search")
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
    object EditProfile : Screen("edit_profile")
    object QrScan : Screen("qr_scan")
    object PhotoPicker : Screen("photo_picker")
    object NotePublish : Screen("note_publish")
    object BrowseHistory : Screen("browse_history")
    object CommentPhotoPicker : Screen("comment_photo_picker")
}
