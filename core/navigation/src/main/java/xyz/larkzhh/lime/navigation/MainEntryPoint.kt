package xyz.larkzhh.lime.navigation

import android.content.Context
import android.content.Intent

object MainEntryPoint {
    var intent: (context: Context, action: String) -> Intent = { context, action ->
        Intent(action).setPackage(context.packageName)
    }
}
