package se.bambulive.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import se.bambulive.data.Prefs
import se.bambulive.data.PrinterState

object WidgetUpdater {
    suspend fun push(ctx: Context, state: PrinterState) {
        Prefs(ctx).widgetState = state.toJson()
        runCatching { BambuWidget().updateAll(ctx) }
    }
}
