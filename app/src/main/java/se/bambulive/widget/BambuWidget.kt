package se.bambulive.widget

import android.content.Context
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import se.bambulive.MainActivity
import se.bambulive.data.ImageStore
import se.bambulive.data.Prefs
import se.bambulive.data.PrinterState
import se.bambulive.data.formatMinutes
import se.bambulive.data.formatTemp
import java.util.Date

class BambuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BambuWidget()
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        RefreshWorker.runNow(context)
    }
}

class BambuWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = Prefs(context)
        val name = prefs.deviceName
        val state = PrinterState.fromJson(prefs.widgetState)
        val image = ImageStore.widgetImage(context)
        val updated = if (state.updatedAt > 0) DateFormat.getTimeFormat(context).format(Date(state.updatedAt)) else null
        provideContent { WidgetContent(name, state, image, updated) }
    }
}

private val BG = Color(0xFF1C1F1D)
private val FG = ColorProvider(Color.White)
private val DIM = ColorProvider(Color(0xFFA9B2AC))
private val ACCENT = Color(0xFF00AE42)

private fun statusColor(s: PrinterState) = when (s.gcodeState) {
    "RUNNING", "PREPARE", "SLICING" -> ACCENT
    "FINISH" -> Color(0xFF4FC3F7)
    "PAUSE" -> Color(0xFFFFB300)
    "FAILED" -> Color(0xFFE53935)
    else -> Color(0xFFA9B2AC)
}

@Composable
private fun WidgetContent(name: String?, s: PrinterState, image: Bitmap?, updated: String?) {
    Row(
        modifier = GlanceModifier.fillMaxSize().background(BG).cornerRadius(18.dp).padding(10.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (image != null) {
            Image(
                provider = ImageProvider(image),
                contentDescription = "Kamerabild",
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.width(118.dp).fillMaxHeight().cornerRadius(12.dp),
            )
            Spacer(GlanceModifier.width(10.dp))
        }
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    name ?: "Öppna appen och logga in",
                    maxLines = 1,
                    style = TextStyle(color = FG, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    "⟳",
                    style = TextStyle(color = DIM, fontSize = 16.sp),
                    modifier = GlanceModifier.padding(start = 6.dp).clickable(actionRunCallback<RefreshAction>()),
                )
            }
            val pct = s.percent?.takeIf { s.isPrinting || s.gcodeState == "FINISH" }
            Text(
                s.statusLabel + (pct?.let { " · $it %" } ?: ""),
                maxLines = 1,
                style = TextStyle(color = ColorProvider(statusColor(s)), fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = (pct ?: 0) / 100f,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = ColorProvider(statusColor(s)),
                backgroundColor = ColorProvider(Color(0xFF3A403C)),
            )
            Spacer(GlanceModifier.height(4.dp))
            val details = buildList {
                if (s.isPrinting) formatMinutes(s.remainingMin)?.let { add("$it kvar") }
                if (s.isPrinting && s.layer != null && s.totalLayers != null && s.totalLayers > 0)
                    add("lager ${s.layer}/${s.totalLayers}")
            }
            if (details.isNotEmpty()) Text(details.joinToString(" · "), maxLines = 1, style = TextStyle(color = FG, fontSize = 12.sp))
            s.jobName?.let { Text(it, maxLines = 1, style = TextStyle(color = DIM, fontSize = 11.sp)) }
            Text(
                "Munstycke ${formatTemp(s.nozzle, s.nozzleTarget)} · Bädd ${formatTemp(s.bed, s.bedTarget)}",
                maxLines = 1,
                style = TextStyle(color = DIM, fontSize = 11.sp),
            )
            Box(modifier = GlanceModifier.defaultWeight()) {}
            Text(
                updated?.let { "Uppdaterad $it" } ?: "Ingen data ännu",
                maxLines = 1,
                style = TextStyle(color = DIM, fontSize = 10.sp),
            )
        }
    }
}
