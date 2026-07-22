package xyz.larkzhh.lime.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Video : Screen("video")
    // object Search : Screen("search")
    object Publish : Screen("publish")
    object Message : Screen("message")
    object Profile : Screen("profile")
    object Detail : Screen("detail/{noteId}") {
        const val ROUTE = "detail/{noteId}"
        fun createRoute(noteId: String) = "detail/$noteId"
    }
    object EditProfile : Screen("edit_profile")
    object PhotoPicker : Screen("photo_picker")
    object NotePublish : Screen("note_publish")
}
