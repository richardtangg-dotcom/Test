package se.bambulive.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppRoot(vm: MainViewModel = viewModel()) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (val s = vm.screen) {
                Screen.Login -> LoginScreen(vm)
                is Screen.Code -> CodeScreen(vm, s)
                is Screen.Devices -> DevicesScreen(vm, s)
                Screen.Dashboard -> DashboardScreen(vm)
                Screen.WebLogin -> WebLoginScreen(vm)
                is Screen.ManualDevice -> ManualDeviceScreen(vm, s)
            }
        }
    }
}

@Composable
fun ErrorText(msg: String?) {
    if (msg != null) Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun LoginScreen(vm: MainViewModel) {
    var email by rememberSaveable { mutableStateOf(vm.prefs.email ?: "") }
    var password by remember { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf(vm.prefs.region) }
    var usePassword by rememberSaveable { mutableStateOf(false) }
    var showToken by rememberSaveable { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Bambu Live", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Logga in med ditt Bambu Lab-konto för att se din P2S var du än är.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            email, { email = it }, label = { Text("E-post (samma som ditt Google-konto)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(region == "global", { region = "global" }, label = { Text("Global") })
            FilterChip(region == "china", { region = "china" }, label = { Text("Kina") })
        }
        ErrorText(vm.error)
        Button(
            onClick = { vm.openWebLogin(region) },
            enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Logga in via Bambu Lab-webben") }
        Text(
            "Rekommenderas. Välj \"logga in med kod\" på sidan om du har Google-konto (Google tillåter inte inloggning i inbäddade webbläsare).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Andra sätt", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        if (!usePassword) {
            OutlinedButton(
                onClick = { vm.startEmailCodeLogin(email, region) },
                enabled = !vm.busy && email.contains('@'),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Skicka inloggningskod till e-post") }
            Text(
                "Fungerar även om du loggar in med Google eller Apple i Bambu Handy – du behöver inget lösenord.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { usePassword = true; vm.error = null }) { Text("Logga in med lösenord i stället") }
        } else {
            OutlinedTextField(
                password, { password = it }, label = { Text("Lösenord") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { vm.login(email, password, region) },
                enabled = !vm.busy && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { if (vm.busy) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Logga in") }
            TextButton(onClick = { usePassword = false; vm.error = null }) { Text("Logga in med e-postkod i stället") }
        }

        TextButton(onClick = { showToken = !showToken }) { Text(if (showToken) "Dölj avancerat" else "Avancerat: klistra in token") }
        if (showToken) {
            Text(
                "Om kodinloggningen inte fungerar: logga in på bambulab.com i en webbläsare och kopiera värdet av cookien \"token\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(token, { token = it }, label = { Text("Token") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { vm.loginWithToken(token, region) },
                enabled = !vm.busy && token.length > 20,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Logga in med token") }
        }
    }
}

@Composable
private fun CodeScreen(vm: MainViewModel, s: Screen.Code) {
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("Verifiering", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (s.tfaKey != null) "Skriv koden från din autentiseringsapp."
            else "Bambu Lab har skickat en kod till ${s.email}. Skriv in den här.",
        )
        OutlinedTextField(
            code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("Kod") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(vm.error)
        Button(
            onClick = { vm.submitCode(code) },
            enabled = !vm.busy && code.length >= 4,
            modifier = Modifier.fillMaxWidth(),
        ) { if (vm.busy) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Fortsätt") }
        Row {
            if (s.tfaKey == null) TextButton(onClick = vm::resendCode, enabled = !vm.busy) { Text("Skicka ny kod") }
            TextButton(onClick = vm::backToLogin) { Text("Tillbaka") }
        }
    }
}

@Composable
private fun DevicesScreen(vm: MainViewModel, s: Screen.Devices) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Välj skrivare", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        ErrorText(vm.error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(s.devices, key = { it.id }) { d ->
                Card(Modifier.fillMaxWidth().clickable { vm.select(d) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.Bold)
                            Text("${d.model} · ${d.id}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            if (d.online) "Online" else "Offline",
                            color = if (d.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualDeviceScreen(vm: MainViewModel, s: Screen.ManualDevice) {
    var serial by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("P2S") }
    var code by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Lägg till skrivaren", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Du är inloggad, men skrivarlistan kunde inte hämtas från Bambu Cloud. Skriv in uppgifterna från skrivarens skärm (Inställningar → Enhet / Nätverk).")
        s.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        OutlinedTextField(serial, { serial = it }, label = { Text("Serienummer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(name, { name = it }, label = { Text("Namn") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(code, { code = it }, label = { Text("Åtkomstkod (för kameran)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ErrorText(vm.error)
        Button(
            onClick = { vm.addManualDevice(serial, name, code) },
            enabled = serial.trim().length >= 8,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Spara") }
        Row {
            TextButton(onClick = vm::loadDevices, enabled = !vm.busy) { Text("Försök hämta listan igen") }
            TextButton(onClick = vm::logout) { Text("Logga ut") }
        }
    }
}
