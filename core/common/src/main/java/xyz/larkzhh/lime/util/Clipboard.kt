package xyz.larkzhh.lime.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun String.copyToClipboard(context: Context) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(null, this))
}
