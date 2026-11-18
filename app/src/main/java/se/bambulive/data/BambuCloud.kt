package se.bambulive.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CloudDevice(
    val id: String,
    val name: String,
    val model: String,
    val online: Boolean,
    val printStatus: String,
    val accessCode: String?,
)

data class CloudTask(val title: String?, val coverUrl: String?)

sealed interface LoginResult {
    data class Success(val token: String) : LoginResult
    data object NeedEmailCode : LoginResult
    data class NeedTfa(val tfaKey: String) : LoginResult
}

/**
 * Klient för Bambu Clouds (inofficiella) HTTP-API – samma anrop som Bambu Studio/Handy gör.
 */
class BambuCloud(region: String) {
    private val china = region == "china"
    private val api = if (china) "https://api.bambulab.cn" else "https://api.bambulab.com"
    private val web = if (china) "https://bambulab.cn" else "https://bambulab.com"

    private val json = "application/json".toMediaType()

    private fun Request.Builder.bblHeaders(token: String? = null) = apply {
        // Bambu ber tredjepartsappar att identifiera sig med en egen User-Agent
        // (gamla "bambu_network_agent/01.09.*" blockeras av deras Cloudflare sedan nov 2026).
        header("User-Agent", "bambulive-android/${se.bambulive.BuildConfig.VERSION_NAME}")
        header("X-BBL-Client-Type", "slicer")
        header("X-BBL-Language", "sv-SE")
        header("Accept", "application/json")
        if (token != null) header("Authorization", "Bearer $token")
    }

    private suspend fun call(req: Request): Pair<JSONObject, List<String>> = withContext(Dispatchers.IO) {
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val obj = runCatching { JSONObject(body) }.getOrElse { JSONObject() }
            if (!resp.isSuccessful) {
                if (body.trimStart().startsWith("<")) {
                    throw IOException(
                        "Bambu Labs Cloudflare-skydd blockerade anropet (${resp.code}, ${req.url.encodedPath})."
                    )
                }
                val msg = obj.optString("message").ifBlank { obj.optString("error") }.ifBlank { body.take(200) }
                throw IOException(
                    when (resp.code) {
                        401, 403 -> "Inloggningen nekades (${resp.code}). $msg"
                        else -> "Bambu Cloud svarade ${resp.code}: $msg"
                    }.trim()
                )
            }
            obj to resp.headers("Set-Cookie")
        }
    }

    private suspend fun post(url: String, body: JSONObject, token: String? = null) =
        call(Request.Builder().url(url).bblHeaders(token).post(body.toString().toRequestBody(json)).build())

    private suspend fun get(path: String, token: String) =
        call(Request.Builder().url(api + path).bblHeaders(token).get().build()).first

    private fun parseLogin(o: JSONObject): LoginResult {
        val token = o.optString("accessToken")
        if (token.isNotBlank()) return LoginResult.Success(token)
        return when (o.optString("loginType")) {
            "verifyCode" -> LoginResult.NeedEmailCode
            "tfa" -> LoginResult.NeedTfa(o.optString("tfaKey"))
            else -> throw IOException(o.optString("message").ifBlank { "Inloggningen misslyckades" })
        }
    }

    suspend fun login(email: String, password: String): LoginResult = parseLogin(
        post("$api/v1/user-service/user/login",
            JSONObject().put("account", email).put("password", password).put("apiError", "")).first
    )

    suspend fun requestEmailCode(email: String) {
        post("$api/v1/user-service/user/sendemail/code",
            JSONObject().put("email", email).put("type", "codeLogin"))
    }

    suspend fun loginWithEmailCode(email: String, code: String): String =
        when (val r = parseLogin(post("$api/v1/user-service/user/login",
            JSONObject().put("account", email).put("code", code.trim())).first)) {
            is LoginResult.Success -> r.token
            else -> throw IOException("Koden godtogs inte")
        }

    suspend fun loginWithTfa(tfaKey: String, code: String): String {
        val (obj, cookies) = post("$web/api/sign-in/tfa",
            JSONObject().put("tfaKey", tfaKey).put("tfaCode", code.trim()))
        cookies.firstNotNullOfOrNull { c ->
            c.split(';').first().trim().takeIf { it.startsWith("token=") }?.removePrefix("token=")
        }?.let { return it }
        return obj.optString("accessToken").ifBlank { throw IOException("Tvåstegskoden godtogs inte") }
    }

    /** Token är en JWT; användarnamnet (u_<uid>) står i den, så vi slipper ett API-anrop. */
    fun uidFromToken(token: String): String? = runCatching {
        val part = token.split('.').getOrNull(1) ?: return null
        val json = JSONObject(String(android.util.Base64.decode(part, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)))
        json.optString("username").takeIf { it.startsWith("u_") }?.removePrefix("u_")
            ?: json.opt("uid")?.toString()?.takeIf { it.isNotBlank() && it != "null" }
    }.getOrNull()

    /** Användar-ID behövs som MQTT-användarnamn (u_<uid>). */
    suspend fun fetchUid(token: String): String {
        val o = get("/v1/design-user-service/my/preference", token)
        val uid = o.opt("uid")?.toString()
        if (uid.isNullOrBlank() || uid == "null") throw IOException("Hittade inget användar-ID i kontot")
        return uid
    }

    suspend fun fetchDevices(token: String): List<CloudDevice> {
        val arr = get("/v1/iot-service/api/user/bind", token).optJSONArray("devices") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val d = arr.getJSONObject(i)
            CloudDevice(
                id = d.optString("dev_id"),
                name = d.optString("name").ifBlank { d.optString("dev_id") },
                model = d.optString("dev_product_name").ifBlank { d.optString("dev_model_name") },
                online = d.optBoolean("online"),
                printStatus = d.optString("print_status"),
                accessCode = d.optString("dev_access_code").ifBlank { null },
            )
        }
    }

    suspend fun fetchLatestTask(token: String, deviceId: String): CloudTask? {
        val hits = get("/v1/user-service/my/tasks?deviceId=$deviceId&limit=1", token).optJSONArray("hits")
        val t = hits?.optJSONObject(0) ?: return null
        return CloudTask(t.optString("title").ifBlank { null }, t.optString("cover").ifBlank { null })
    }

    suspend fun download(url: String): ByteArray = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("Nedladdning misslyckades (${r.code})")
            r.body!!.bytes()
        }
    }

    companion object {
        val http: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        fun mqttHost(region: String) = if (region == "china") "cn.mqtt.bambulab.com" else "us.mqtt.bambulab.com"
    }
}
