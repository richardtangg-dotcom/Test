package se.bambulive.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

/** Sparar senaste kamerabild och omslagsbild (från molnet) som widgeten visar. */
object ImageStore {
    private const val SNAPSHOT_MAX_AGE_MS = 15 * 60 * 1000L

    private fun snapshotFile(ctx: Context) = File(ctx.filesDir, "snapshot.jpg")
    private fun coverFile(ctx: Context) = File(ctx.filesDir, "cover.jpg")

    fun saveSnapshot(ctx: Context, bmp: Bitmap) {
        val scaled = scaleDown(bmp, 640)
        val tmp = File(ctx.filesDir, "snapshot.tmp")
        tmp.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        tmp.renameTo(snapshotFile(ctx))
        Prefs(ctx).snapshotTime = System.currentTimeMillis()
    }

    /** Laddar ner omslagsbilden för senaste utskriften från Bambu Cloud om den ändrats. */
    suspend fun refreshCover(ctx: Context) {
        val prefs = Prefs(ctx)
        val token = prefs.token ?: return
        val dev = prefs.deviceId ?: return
        runCatching {
            val task = BambuCloud(prefs.region).fetchLatestTask(token, dev) ?: return
            val url = task.coverUrl ?: return
            // Omslagsadresserna är signerade och byter query-sträng; jämför utan den.
            val key = url.substringBefore('?')
            if (key == prefs.coverUrl && coverFile(ctx).exists()) return
            val bytes = BambuCloud(prefs.region).download(url)
            coverFile(ctx).writeBytes(bytes)
            prefs.coverUrl = key
        }.onFailure { Log.w("ImageStore", "Kunde inte hämta omslagsbild", it) }
    }

    /** Färsk kamerabild om en finns, annars omslagsbilden. */
    fun widgetImage(ctx: Context, maxPx: Int = 400): Bitmap? {
        val fresh = System.currentTimeMillis() - Prefs(ctx).snapshotTime < SNAPSHOT_MAX_AGE_MS
        val file = listOf(snapshotFile(ctx).takeIf { fresh }, coverFile(ctx), snapshotFile(ctx))
            .firstOrNull { it != null && it.exists() } ?: return null
        val bmp = runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() ?: return null
        return scaleDown(bmp, maxPx)
    }

    fun clear(ctx: Context) {
        snapshotFile(ctx).delete()
        coverFile(ctx).delete()
    }

    private fun scaleDown(bmp: Bitmap, maxPx: Int): Bitmap {
        val largest = maxOf(bmp.width, bmp.height)
        if (largest <= maxPx) return bmp
        val f = maxPx.toFloat() / largest
        return Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt(), (bmp.height * f).toInt(), true)
    }
}
