package se.bambulive.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import se.bambulive.MonitorService
import se.bambulive.data.BambuCloud
import se.bambulive.data.CloudDevice
import se.bambulive.data.ImageStore
import se.bambulive.data.LoginResult
import se.bambulive.data.Prefs
import se.bambulive.data.PrinterRepository
import se.bambulive.data.PrinterState
import se.bambulive.widget.RefreshWorker
import se.bambulive.widget.WidgetUpdater

sealed interface Screen {
    data object Login : Screen
    data class Code(val email: String, val tfaKey: String?) : Screen
    data class Devices(val devices: List<CloudDevice>) : Screen
    data object Dashboard : Screen
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)

    var screen by mutableStateOf<Screen>(
        when {
            prefs.hasDevice -> Screen.Dashboard
            else -> Screen.Login
        }
    )
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    private val cloud get() = BambuCloud(prefs.region)

    init {
        if (prefs.loggedIn && !prefs.hasDevice) loadDevices()
    }

    private fun run(block: suspend () -> Unit) = viewModelScope.launch {
        busy = true
        error = null
        try {
            block()
        } catch (e: Exception) {
            error = e.message ?: e.toString()
        } finally {
            busy = false
        }
    }

    fun login(email: String, password: String, region: String) = run {
        prefs.region = region
        prefs.email = email.trim()
        when (val r = cloud.login(email.trim(), password)) {
            is LoginResult.Success -> finishLogin(r.token)
            LoginResult.NeedEmailCode -> {
                cloud.requestEmailCode(email.trim())
                screen = Screen.Code(email.trim(), null)
            }
            is LoginResult.NeedTfa -> screen = Screen.Code(email.trim(), r.tfaKey)
        }
    }

    fun resendCode() {
        val s = screen as? Screen.Code ?: return
        if (s.tfaKey == null) run { cloud.requestEmailCode(s.email) }
    }

    fun submitCode(code: String) {
        val s = screen as? Screen.Code ?: return
        run {
            val token = if (s.tfaKey != null) cloud.loginWithTfa(s.tfaKey, code)
            else cloud.loginWithEmailCode(s.email, code)
            finishLogin(token)
        }
    }

    fun backToLogin() {
        error = null
        screen = Screen.Login
    }

    private suspend fun finishLogin(token: String) {
        prefs.token = token
        prefs.uid = cloud.fetchUid(token)
        fetchDevices()
    }

    fun loadDevices() = run { fetchDevices() }

    private suspend fun fetchDevices() {
        val devices = cloud.fetchDevices(prefs.token ?: return)
        if (devices.isEmpty()) throw IllegalStateException("Inga skrivare hittades på kontot.")
        val p2s = devices.filter { it.model.contains("P2S", ignoreCase = true) }
        if (devices.size == 1 || p2s.size == 1 && prefs.deviceId == null) select(p2s.singleOrNull() ?: devices.first())
        else screen = Screen.Devices(devices)
    }

    fun select(d: CloudDevice) {
        val changed = d.id != prefs.deviceId
        if (changed) {
            prefs.clearDevice()
            ImageStore.clear(getApplication())
        }
        prefs.deviceId = d.id
        prefs.deviceName = d.name
        if (d.accessCode != null) prefs.accessCode = d.accessCode
        if (changed) PrinterRepository.reset()
        RefreshWorker.schedule(getApplication())
        RefreshWorker.runNow(getApplication())
        screen = Screen.Dashboard
    }

    fun switchPrinter() = loadDevices()

    fun logout() {
        MonitorService.stop(getApplication())
        RefreshWorker.cancel(getApplication())
        prefs.logout()
        ImageStore.clear(getApplication())
        PrinterRepository.reset()
        viewModelScope.launch { WidgetUpdater.push(getApplication(), PrinterState()) }
        screen = Screen.Login
    }
}
