package se.bambulive.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import se.bambulive.R
import se.bambulive.data.ImageStore
import se.bambulive.data.PrinterRepository
import se.bambulive.data.RtspsProxy
import se.bambulive.widget.WidgetUpdater
import kotlin.concurrent.thread

/**
 * Visar skrivarens kamera (RTSPS på port 322 i det lokala nätverket) och sparar
 * en bild var 10:e sekund som widgeten kan visa.
 */
@OptIn(UnstableApi::class)
@Composable
fun LiveCamera(ip: String, accessCode: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var attempt by remember { mutableIntStateOf(0) }
    var status by remember(ip, accessCode, attempt) { mutableStateOf<String?>(null) }
    var loading by remember(ip, accessCode, attempt) { mutableStateOf(true) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    val player = remember(ip, accessCode, attempt) { ExoPlayer.Builder(ctx).build() }

    DisposableEffect(player) {
        val proxy = RtspsProxy(ip).start()
        val url = "rtsp://bblp:${Uri.encode(accessCode)}@127.0.0.1:${proxy.localPort}/streaming/live/1"
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) { loading = false; status = null }
            }
            override fun onPlayerError(error: PlaybackException) {
                loading = false
                status = "Kunde inte visa kameran (${error.errorCodeName}).\n" +
                    "Kameran nås bara när mobilen är på samma WiFi som skrivaren, och " +
                    "\"LAN Mode Liveview\" måste vara påslaget på skrivaren."
            }
        })
        player.setMediaSource(
            RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .setTimeoutMs(10_000)
                .createMediaSource(MediaItem.fromUri(url))
        )
        player.volume = 0f
        player.playWhenReady = true
        player.prepare()
        onDispose {
            player.release()
            thread { proxy.close() }
        }
    }

    // Spara bilder åt widgeten medan videon spelas.
    LaunchedEffect(player) {
        while (true) {
            delay(10_000)
            if (loading || status != null) continue
            val bmp = (playerView?.videoSurfaceView as? TextureView)?.bitmap ?: continue
            withContext(Dispatchers.IO) { ImageStore.saveSnapshot(ctx, bmp) }
            WidgetUpdater.push(ctx, PrinterRepository.state.value)
        }
    }

    Box(
        modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { c ->
                (LayoutInflater.from(c).inflate(R.layout.player_view, null) as PlayerView).also { playerView = it }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        if (loading) CircularProgressIndicator()
        status?.let { msg ->
            Column(
                Modifier.fillMaxSize().background(Color(0xCC000000)).padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(msg, color = Color.White, textAlign = TextAlign.Center)
                OutlinedButton(onClick = { attempt++ }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Försök igen")
                }
            }
        }
    }
}
