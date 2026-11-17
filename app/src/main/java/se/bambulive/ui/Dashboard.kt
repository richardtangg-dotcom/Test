package se.bambulive.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.bambulive.MonitorService
import se.bambulive.data.PrinterRepository
import se.bambulive.data.PrinterState
import se.bambulive.data.formatMinutes
import se.bambulive.data.formatTemp

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val state by PrinterRepository.state.collectAsStateWithLifecycle()
    val live by MonitorService.running.collectAsStateWithLifecycle()

    LifecycleStartEffect(Unit) {
        PrinterRepository.acquire()
        PrinterRepository.requestPushAll()
        onStopOrDispose { PrinterRepository.release() }
    }

    var manualIp by remember { mutableStateOf(vm.prefs.manualIp) }
    var accessCode by remember { mutableStateOf(vm.prefs.accessCode) }
    var cameraOn by rememberSaveable { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    val ip = manualIp ?: state.ip

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        MonitorService.start(ctx)
    }
    fun startLive() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else MonitorService.start(ctx)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(vm.prefs.deviceName ?: "Skrivare", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (state.connected) "Ansluten via Bambu Cloud" else "Ansluter till Bambu Cloud…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(state.statusLabel, color = statusColor(state), fontWeight = FontWeight.Bold)
        }
        ErrorText(state.error ?: vm.error)

        // Kamera
        val shape = RoundedCornerShape(12.dp)
        when {
            !cameraOn -> CameraPlaceholder("Kameran är pausad", shape)
            ip == null || accessCode.isNullOrBlank() -> CameraPlaceholder(
                if (accessCode.isNullOrBlank()) "Ange skrivarens åtkomstkod under Kamerainställningar för att se kameran."
                else "Väntar på skrivarens IP-adress…", shape,
            )
            else -> LiveCamera(ip, accessCode!!, Modifier.clip(shape))
        }
        if (state.liveviewDisabled) {
            Text(
                "Skrivaren rapporterar att liveview på LAN är avstängt. Slå på \"LAN Mode Liveview\" i skrivarens inställningar (Nätverk).",
                color = Color(0xFFFFB300), style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { cameraOn = !cameraOn }) { Text(if (cameraOn) "Pausa kamera" else "Visa kamera") }
            OutlinedButton(onClick = { showSettings = true }) { Text("Kamerainställningar") }
        }

        // Utskrift
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.jobName ?: "Ingen utskrift", fontWeight = FontWeight.Bold)
                val pct = state.percent ?: 0
                LinearProgressIndicator(
                    progress = { pct / 100f },
                    color = statusColor(state),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row {
                    Text("$pct %", Modifier.weight(1f))
                    formatMinutes(state.remainingMin)?.takeIf { state.isPrinting }?.let { Text("$it kvar") }
                }
                if (state.layer != null && state.totalLayers != null && state.totalLayers!! > 0) {
                    Text("Lager ${state.layer} av ${state.totalLayers}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp)) {
                Stat("Munstycke", formatTemp(state.nozzle, state.nozzleTarget), Modifier.weight(1f))
                Stat("Bädd", formatTemp(state.bed, state.bedTarget), Modifier.weight(1f))
                Stat("Kammare", formatTemp(state.chamber, null), Modifier.weight(1f))
            }
        }

        // Live-widget
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Live-widget", fontWeight = FontWeight.Bold)
                    Text(
                        if (live) "Widgeten uppdateras var 5:e sekund."
                        else "Av: widgeten uppdateras var 15:e minut. Slå på under en utskrift.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = live, onCheckedChange = { on -> if (on) startLive() else MonitorService.stop(ctx) })
            }
        }
        Text(
            "Lägg till widgeten genom att hålla in på hemskärmen → Widgetar → Bambu Live.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = vm::switchPrinter) { Text("Byt skrivare") }
            TextButton(onClick = vm::logout) { Text("Logga ut") }
        }
    }

    if (showSettings) {
        CameraSettingsDialog(
            ip = manualIp ?: "",
            reportedIp = state.ip,
            code = accessCode ?: "",
            onDismiss = { showSettings = false },
            onSave = { newIp, newCode ->
                vm.prefs.manualIp = newIp
                vm.prefs.accessCode = newCode.trim().ifBlank { null }
                manualIp = vm.prefs.manualIp
                accessCode = vm.prefs.accessCode
                showSettings = false
            },
        )
    }
}

@Composable
private fun CameraPlaceholder(text: String, shape: RoundedCornerShape) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(Color(0xFF1C1F1D)).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun statusColor(s: PrinterState) = when (s.gcodeState) {
    "RUNNING", "PREPARE", "SLICING" -> MaterialTheme.colorScheme.primary
    "FINISH" -> Color(0xFF4FC3F7)
    "PAUSE" -> Color(0xFFFFB300)
    "FAILED" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun CameraSettingsDialog(
    ip: String, reportedIp: String?, code: String,
    onDismiss: () -> Unit, onSave: (String, String) -> Unit,
) {
    var newIp by remember { mutableStateOf(ip) }
    var newCode by remember { mutableStateOf(code) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kamerainställningar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Åtkomstkoden hämtas från ditt konto, men finns också på skrivarens skärm under Inställningar → Nätverk.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(newCode, { newCode = it }, label = { Text("Åtkomstkod") }, singleLine = true)
                OutlinedTextField(
                    newIp, { newIp = it },
                    label = { Text("IP-adress (valfri)") },
                    placeholder = { Text(reportedIp ?: "t.ex. 192.168.1.50") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Text(
                    "Lämna IP tomt för att använda adressen skrivaren rapporterar" +
                        (reportedIp?.let { " ($it)" } ?: "") + ".",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(newIp, newCode) }) { Text("Spara") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
