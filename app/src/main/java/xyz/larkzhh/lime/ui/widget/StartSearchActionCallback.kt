package xyz.larkzhh.lime.ui.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import xyz.larkzhh.lime.MainActivity
import xyz.larkzhh.lime.navigation.ShortcutActions

class StartSearchActionCallback : ActionCallback {

    companion object {
        val KEYWORD = ActionParameters.Key<String>("keyword")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val keyword = parameters[KEYWORD] ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ShortcutActions.SEARCH_KEYWORD
            putExtra(ShortcutActions.EXTRA_KEYWORD, keyword)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}
