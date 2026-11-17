package se.bambulive.data

import android.net.Uri
import org.json.JSONObject

/** Senast kända status för skrivaren, byggd av MQTT-rapporterna (som ofta bara innehåller ändringar). */
data class PrinterState(
    val connected: Boolean = false,
    val gcodeState: String? = null,
    val percent: Int? = null,
    val remainingMin: Int? = null,
    val layer: Int? = null,
    val totalLayers: Int? = null,
    val nozzle: Double? = null,
    val nozzleTarget: Double? = null,
    val bed: Double? = null,
    val bedTarget: Double? = null,
    val chamber: Double? = null,
    val jobName: String? = null,
    val ip: String? = null,
    val rtspUrl: String? = null,
    val liveviewDisabled: Boolean = false,
    val error: String? = null,
    val updatedAt: Long = 0,
) {
    val isPrinting get() = gcodeState in setOf("RUNNING", "PREPARE", "SLICING", "PAUSE")

    val statusLabel: String
        get() = when (gcodeState) {
            "RUNNING" -> "Skriver ut"
            "PREPARE" -> "Förbereder"
            "SLICING" -> "Slicar"
            "PAUSE" -> "Pausad"
            "FINISH" -> "Klar"
            "FAILED" -> "Misslyckades"
            "IDLE" -> "Redo"
            null -> if (connected) "Väntar på data…" else "Ingen kontakt"
            else -> gcodeState.lowercase().replaceFirstChar { it.uppercase() }
        }

    fun merge(p: JSONObject, now: Long = System.currentTimeMillis()): PrinterState {
        fun d(k: String) = if (p.has(k)) p.optDouble(k).takeUnless { it.isNaN() } else null
        fun i(k: String) = if (p.has(k)) p.optInt(k, Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE } else null
        fun s(k: String) = if (p.has(k)) p.optString(k).takeIf { it.isNotBlank() } else null

        // Nyare firmware (H2D/P2S) kan packa temperaturer som (mål << 16) | aktuell i "device".
        var nozzleNew: Double? = null; var nozzleTgtNew: Double? = null
        var bedNew: Double? = null; var bedTgtNew: Double? = null
        var chamberNew: Double? = null
        p.optJSONObject("device")?.let { dev ->
            dev.optJSONObject("extruder")?.optJSONArray("info")?.optJSONObject(0)?.let { e ->
                if (e.has("temp")) e.optLong("temp").let { nozzleNew = (it and 0xFFFF).toDouble(); nozzleTgtNew = (it shr 16 and 0xFFFF).toDouble() }
            }
            dev.optJSONObject("bed")?.optJSONObject("info")?.let { b ->
                if (b.has("temp")) b.optLong("temp").let { bedNew = (it and 0xFFFF).toDouble(); bedTgtNew = (it shr 16 and 0xFFFF).toDouble() }
            }
            dev.optJSONObject("ctc")?.optJSONObject("info")?.let { c ->
                if (c.has("temp")) chamberNew = (c.optLong("temp") and 0xFFFF).toDouble()
            }
        }

        var rtsp = rtspUrl
        var disabled = liveviewDisabled
        p.optJSONObject("ipcam")?.optString("rtsp_url")?.let { url ->
            when {
                url.startsWith("rtsp") -> { rtsp = url; disabled = false }
                url == "disable" -> { rtsp = null; disabled = true }
            }
        }

        var newIp = ip
        p.optJSONObject("net")?.optJSONArray("info")?.let { arr ->
            for (n in 0 until arr.length()) {
                val v = arr.optJSONObject(n)?.optLong("ip") ?: 0L
                if (v != 0L) {
                    // Little-endian heltal
                    newIp = "${v and 0xFF}.${v shr 8 and 0xFF}.${v shr 16 and 0xFF}.${v shr 24 and 0xFF}"
                    break
                }
            }
        }
        rtsp?.let { runCatching { Uri.parse(it).host }.getOrNull() }?.takeIf { it.isNotBlank() }?.let { newIp = it }

        return copy(
            connected = true,
            gcodeState = s("gcode_state") ?: gcodeState,
            percent = i("mc_percent") ?: percent,
            remainingMin = i("mc_remaining_time") ?: remainingMin,
            layer = i("layer_num") ?: layer,
            totalLayers = i("total_layer_num") ?: totalLayers,
            nozzle = d("nozzle_temper") ?: nozzleNew ?: nozzle,
            nozzleTarget = d("nozzle_target_temper") ?: nozzleTgtNew ?: nozzleTarget,
            bed = d("bed_temper") ?: bedNew ?: bed,
            bedTarget = d("bed_target_temper") ?: bedTgtNew ?: bedTarget,
            chamber = d("chamber_temper") ?: chamberNew ?: chamber,
            jobName = s("subtask_name") ?: s("gcode_file")?.substringAfterLast('/') ?: jobName,
            ip = newIp,
            rtspUrl = rtsp,
            liveviewDisabled = disabled,
            error = null,
            updatedAt = now,
        )
    }

    fun toJson(): String = JSONObject().apply {
        put("connected", connected)
        put("gcodeState", gcodeState); put("percent", percent); put("remainingMin", remainingMin)
        put("layer", layer); put("totalLayers", totalLayers)
        put("nozzle", nozzle); put("nozzleTarget", nozzleTarget)
        put("bed", bed); put("bedTarget", bedTarget); put("chamber", chamber)
        put("jobName", jobName); put("ip", ip); put("rtspUrl", rtspUrl)
        put("liveviewDisabled", liveviewDisabled); put("error", error); put("updatedAt", updatedAt)
    }.toString()

    companion object {
        fun fromJson(s: String?): PrinterState {
            if (s.isNullOrBlank()) return PrinterState()
            return runCatching {
                val o = JSONObject(s)
                fun d(k: String) = if (o.isNull(k)) null else o.optDouble(k)
                fun i(k: String) = if (o.isNull(k)) null else o.optInt(k)
                fun t(k: String) = if (o.isNull(k)) null else o.optString(k)
                PrinterState(
                    connected = false,
                    gcodeState = t("gcodeState"), percent = i("percent"), remainingMin = i("remainingMin"),
                    layer = i("layer"), totalLayers = i("totalLayers"),
                    nozzle = d("nozzle"), nozzleTarget = d("nozzleTarget"),
                    bed = d("bed"), bedTarget = d("bedTarget"), chamber = d("chamber"),
                    jobName = t("jobName"), ip = t("ip"), rtspUrl = t("rtspUrl"),
                    liveviewDisabled = o.optBoolean("liveviewDisabled"), error = t("error"),
                    updatedAt = o.optLong("updatedAt"),
                )
            }.getOrDefault(PrinterState())
        }
    }
}

fun formatMinutes(min: Int?): String? {
    if (min == null || min < 0) return null
    val h = min / 60
    val m = min % 60
    return if (h > 0) "$h h $m min" else "$m min"
}

fun formatTemp(cur: Double?, target: Double?): String {
    if (cur == null) return "–"
    val c = "${cur.toInt()}°"
    return if (target != null && target > 0) "$c/${target.toInt()}°" else c
}
