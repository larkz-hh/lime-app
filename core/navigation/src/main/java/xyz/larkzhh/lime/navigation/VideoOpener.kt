package xyz.larkzhh.lime.navigation

import android.content.Context
import android.content.Intent

object VideoOpener {
    var open: (context: Context, noteId: Long, source: String) -> Unit = { _, _, _ -> }

    var intent: ((context: Context, noteId: Long, source: String) -> Intent)? = null
}
