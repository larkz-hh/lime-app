package xyz.larkzhh.lime.util

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import es.dmoral.toasty.Toasty

fun String.showToast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toasty.custom(
        context,
        this,
        null,
        Color.BLACK,
        Color.WHITE,
        duration,
        false,
        true,
    ).show()
}