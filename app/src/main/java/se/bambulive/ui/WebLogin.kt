package se.bambulive.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Inloggning på Bambu Labs egen webbsida i en inbäddad webbläsare. Sidan sätter kakan
 * "token" när man är inloggad – den används sedan mot molnet och MQTT.
 * En riktig webbläsare kommer förbi Cloudflare-skyddet som stoppar direkta API-anrop.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginScreen(vm: MainViewModel) {
    val china = vm.prefs.region == "china"
    val site = if (china) "https://bambulab.cn" else "https://bambulab.com"
    val startUrl = if (china) "$site/zh/sign-in" else "$site/en/sign-in"
    var done by remember { mutableStateOf(false) }

    fun findToken(): String? {
        val cookies = CookieManager.getInstance().getCookie(site) ?: return null
        return cookies.split(';').map { it.trim() }
            .firstOrNull { it.startsWith("token=") }
            ?.removePrefix("token=")
            ?.takeIf { it.length > 20 }
    }

    fun check() {
        if (done) return
        findToken()?.let { done = true; vm.onWebToken(it) }
    }

    LaunchedEffect(Unit) {
        while (!done) {
            delay(1000)
            check()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (done) "Inloggad – hämtar skrivare…" else "Logga in på Bambu Lab",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(8.dp),
            )
            if (done || vm.busy) CircularProgressIndicator(Modifier.padding(8.dp))
            TextButton(onClick = vm::backToLogin) { Text("Avbryt") }
        }
        ErrorText(vm.error)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    removeAllCookies(null)
                    flush()
                }
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Ta bort "; wv" så att sidan ser ut som vanliga Chrome.
                    settings.userAgentString = settings.userAgentString.replace("; wv", "")
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) = check()
                    }
                    loadUrl(startUrl)
                }
            },
        )
    }
}
