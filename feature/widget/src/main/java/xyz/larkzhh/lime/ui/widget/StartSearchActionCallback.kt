package xyz.larkzhh.lime.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import xyz.larkzhh.lime.navigation.MainEntryPoint
import xyz.larkzhh.lime.navigation.action.ShortcutActions

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
        val intent = MainEntryPoint.intent(context, ShortcutActions.SEARCH_KEYWORD).apply {
            putExtra(ShortcutActions.EXTRA_KEYWORD, keyword)
        }
        context.startActivity(intent)
    }
}
