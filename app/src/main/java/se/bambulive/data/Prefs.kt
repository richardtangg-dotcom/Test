package se.bambulive.data

import android.content.Context
import androidx.core.content.edit

/** Små inställningar som sparas privat i appen. */
class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("bambu", Context.MODE_PRIVATE)

    private fun str(key: String) = p.getString(key, null)
    private fun put(key: String, value: String?) = p.edit { putString(key, value) }

    /** "global" eller "china". */
    var region: String
        get() = str("region") ?: "global"
        set(v) = put("region", v)
    var email: String?
        get() = str("email"); set(v) = put("email", v)
    var token: String?
        get() = str("token"); set(v) = put("token", v)
    var uid: String?
        get() = str("uid"); set(v) = put("uid", v)

    var deviceId: String?
        get() = str("deviceId"); set(v) = put("deviceId", v)
    var deviceName: String?
        get() = str("deviceName"); set(v) = put("deviceName", v)
    var accessCode: String?
        get() = str("accessCode"); set(v) = put("accessCode", v)

    /** IP som användaren skrivit in manuellt (annars används IP:n skrivaren rapporterar). */
    var manualIp: String?
        get() = str("manualIp"); set(v) = put("manualIp", v?.trim()?.ifBlank { null })

    var widgetState: String?
        get() = str("widgetState"); set(v) = put("widgetState", v)
    var coverUrl: String?
        get() = str("coverUrl"); set(v) = put("coverUrl", v)
    var snapshotTime: Long
        get() = p.getLong("snapshotTime", 0); set(v) = p.edit { putLong("snapshotTime", v) }

    val loggedIn get() = token != null && uid != null
    val hasDevice get() = loggedIn && deviceId != null

    fun clearDevice() = p.edit {
        listOf("deviceId", "deviceName", "accessCode", "manualIp", "widgetState", "coverUrl", "snapshotTime")
            .forEach { remove(it) }
    }

    fun logout() {
        val keepRegion = region
        val keepEmail = email
        p.edit { clear() }
        region = keepRegion
        email = keepEmail
    }
}
