package xyz.larkzhh.lime.navigation

import android.content.Context

object VideoOpener {
    var open: (context: Context, noteId: Long, source: String) -> Unit = { _, _, _ -> }
}
