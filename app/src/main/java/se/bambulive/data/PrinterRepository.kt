package se.bambulive.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.util.UUID
import javax.net.ssl.SSLSocketFactory

/**
 * Håller en MQTT-anslutning till Bambu Cloud så länge någon (appen, tjänsten eller widget-jobbet)
 * behöver den, och exponerar skrivarens status som ett StateFlow.
 */
object PrinterRepository {
    private const val TAG = "PrinterRepository"

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(PrinterState())
    val state: StateFlow<PrinterState> = _state.asStateFlow()

    private var client: MqttClient? = null
    private var users = 0
    private var generation = 0

    fun init(context: Context) {
        appContext = context.applicationContext
        _state.value = PrinterState.fromJson(Prefs(appContext).widgetState)
    }

    @Synchronized
    fun acquire() {
        users++
        if (client == null) connect()
    }

    @Synchronized
    fun release() {
        users = (users - 1).coerceAtLeast(0)
        if (users == 0) disconnect()
    }

    /** Anropas när användaren byter skrivare eller loggar ut. */
    @Synchronized
    fun reset() {
        disconnect()
        _state.value = PrinterState()
        if (users > 0) connect()
    }

    fun requestPushAll() {
        val c = client ?: return
        val id = Prefs(appContext).deviceId ?: return
        scope.launch {
            runCatching {
                if (c.isConnected) {
                    val msg = """{"pushing":{"sequence_id":"0","command":"pushall","version":1,"push_target":1}}"""
                    c.publish("device/$id/request", MqttMessage(msg.toByteArray()).apply { qos = 0 })
                }
            }.onFailure { Log.w(TAG, "pushall misslyckades", it) }
        }
    }

    @Synchronized
    private fun disconnect() {
        generation++
        val c = client ?: return
        client = null
        scope.launch {
            runCatching { if (c.isConnected) c.disconnect(2000) }
            runCatching { c.close(true) }
        }
        _state.update { it.copy(connected = false) }
    }

    @Synchronized
    private fun connect() {
        val prefs = Prefs(appContext)
        val token = prefs.token
        val uid = prefs.uid
        val deviceId = prefs.deviceId
        if (token == null || uid == null || deviceId == null) return

        val myGen = ++generation
        val c = MqttClient(
            "ssl://${BambuCloud.mqttHost(prefs.region)}:8883",
            "bambulive_" + UUID.randomUUID().toString().take(12),
            MemoryPersistence(),
        )
        client = c
        val topic = "device/$deviceId/report"

        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                runCatching {
                    c.subscribe(topic, 0)
                    _state.update { it.copy(connected = true, error = null) }
                    requestPushAll()
                }.onFailure { Log.w(TAG, "subscribe", it) }
            }

            override fun connectionLost(cause: Throwable?) {
                Log.w(TAG, "Anslutningen bröts", cause)
                _state.update { it.copy(connected = false) }
            }

            override fun messageArrived(t: String?, message: MqttMessage) {
                if (myGen != generation) return
                val obj = runCatching { JSONObject(String(message.payload)) }.getOrNull() ?: return
                val print = obj.optJSONObject("print") ?: return
                _state.update { it.merge(print) }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        val opts = MqttConnectOptions().apply {
            userName = "u_$uid"
            password = token.toCharArray()
            isCleanSession = true
            isAutomaticReconnect = true
            keepAliveInterval = 30
            connectionTimeout = 15
            socketFactory = SSLSocketFactory.getDefault()
        }

        scope.launch {
            try {
                c.connect(opts)
            } catch (e: MqttException) {
                Log.w(TAG, "connect", e)
                if (myGen != generation) return@launch
                val msg = when (e.reasonCode.toShort()) {
                    MqttException.REASON_CODE_FAILED_AUTHENTICATION,
                    MqttException.REASON_CODE_NOT_AUTHORIZED ->
                        "Bambu Cloud nekade anslutningen – logga ut och in igen."
                    else -> "Kunde inte ansluta till Bambu Cloud (${e.reasonCode}). Försöker igen…"
                }
                _state.update { it.copy(connected = false, error = msg) }
                // Paho återansluter bara automatiskt efter en lyckad första anslutning.
                if (e.reasonCode.toShort() != MqttException.REASON_CODE_FAILED_AUTHENTICATION &&
                    e.reasonCode.toShort() != MqttException.REASON_CODE_NOT_AUTHORIZED
                ) {
                    kotlinx.coroutines.delay(15_000)
                    synchronized(this@PrinterRepository) {
                        if (myGen == generation && users > 0) {
                            client = null
                            runCatching { c.close(true) }
                            connect()
                        }
                    }
                }
            }
        }
    }
}
