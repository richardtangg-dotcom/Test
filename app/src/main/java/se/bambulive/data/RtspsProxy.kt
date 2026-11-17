package se.bambulive.data

import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread

/**
 * Skrivaren skickar kamerabilden som RTSPS (RTSP över TLS, port 322) med ett självsignerat
 * certifikat. Media3 kan bara vanlig RTSP, så den här lilla proxyn lyssnar på 127.0.0.1,
 * sköter TLS mot skrivaren och skriver om rtsps://-adresser i RTSP-meddelandena.
 * Spelaren måste köra RTP över TCP (interleaved) så att all trafik går genom samma anslutning.
 */
class RtspsProxy(private val printerHost: String, private val printerPort: Int = 322) {
    private val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
    private val sockets = mutableListOf<Socket>()
    @Volatile private var closed = false

    val localPort: Int get() = server.localPort
    private val localBase get() = "rtsp://127.0.0.1:$localPort"
    private val remoteBase get() = "rtsps://$printerHost:$printerPort"

    fun start(): RtspsProxy {
        thread(name = "rtsps-accept", isDaemon = true) {
            while (!closed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                thread(name = "rtsps-conn", isDaemon = true) { handle(client) }
            }
        }
        return this
    }

    fun close() {
        closed = true
        runCatching { server.close() }
        synchronized(sockets) { sockets.forEach { runCatching { it.close() } }; sockets.clear() }
    }

    private fun handle(client: Socket) {
        val tls = try {
            (trustAll.socketFactory.createSocket() as SSLSocket).apply {
                connect(InetSocketAddress(printerHost, printerPort), 6000)
                soTimeout = 0
                startHandshake()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Kunde inte nå skrivarens kamera", e)
            runCatching { client.close() }
            return
        }
        synchronized(sockets) { sockets += client; sockets += tls }
        val closeBoth = { runCatching { client.close() }; runCatching { tls.close() }; Unit }
        thread(name = "rtsps-up", isDaemon = true) {
            runCatching { pump(client.getInputStream(), tls.outputStream, toPrinter = true) }
            closeBoth()
        }
        runCatching { pump(tls.inputStream, client.getOutputStream(), toPrinter = false) }
        closeBoth()
    }

    private fun pump(rawIn: InputStream, out: OutputStream, toPrinter: Boolean) {
        val input = BufferedInputStream(rawIn, 64 * 1024)
        while (true) {
            val first = input.read()
            if (first < 0) return
            if (first == '$'.code) {
                // Interleaved RTP/RTCP: '$' kanal längd(2) data
                val ch = input.read()
                val hi = input.read()
                val lo = input.read()
                if (lo < 0) return
                val len = (hi shl 8) or lo
                val data = input.readExactly(len) ?: return
                out.write(byteArrayOf('$'.code.toByte(), ch.toByte(), hi.toByte(), lo.toByte()))
                out.write(data)
                out.flush()
            } else {
                val head = ByteArrayOutputStream().apply { write(first) }
                var last4 = first
                while (true) {
                    val b = input.read()
                    if (b < 0) return
                    head.write(b)
                    last4 = (last4 shl 8) or b
                    if (last4 == 0x0D0A0D0A) break
                    if (head.size() > 64 * 1024) return
                }
                val headText = head.toString(Charsets.ISO_8859_1.name())
                val contentLength = Regex("(?im)^content-length:\\s*(\\d+)").find(headText)
                    ?.groupValues?.get(1)?.toInt() ?: 0
                val body = if (contentLength > 0) input.readExactly(contentLength) ?: return else ByteArray(0)

                var newBody = String(body, Charsets.ISO_8859_1)
                newBody = rewrite(newBody, toPrinter)
                val bodyBytes = newBody.toByteArray(Charsets.ISO_8859_1)

                val newHead = headText.trimEnd('\r', '\n').split("\r\n").joinToString("\r\n") { line ->
                    when {
                        line.startsWith("Authorization:", ignoreCase = true) -> line
                        line.startsWith("Content-Length:", ignoreCase = true) && contentLength > 0 ->
                            "Content-Length: ${bodyBytes.size}"
                        else -> rewrite(line, toPrinter)
                    }
                } + "\r\n\r\n"

                out.write(newHead.toByteArray(Charsets.ISO_8859_1))
                if (bodyBytes.isNotEmpty()) out.write(bodyBytes)
                out.flush()
            }
        }
    }

    private fun rewrite(s: String, toPrinter: Boolean): String =
        if (toPrinter) s.replace(LOCAL_URL, remoteBase)
        else s.replace(REMOTE_URL, localBase)

    private fun InputStream.readExactly(n: Int): ByteArray? {
        val buf = ByteArray(n)
        var off = 0
        while (off < n) {
            val r = read(buf, off, n - off)
            if (r < 0) return null
            off += r
        }
        return buf
    }

    companion object {
        private const val TAG = "RtspsProxy"
        private val LOCAL_URL = Regex("rtsp://(127\\.0\\.0\\.1|localhost)(:\\d+)?")
        private val REMOTE_URL = Regex("rtsps://[^/\\s\"';]+")

        /** Skrivaren har ett självsignerat certifikat – används bara mot den lokala skrivaren. */
        private val trustAll: SSLContext by lazy {
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }), SecureRandom())
            }
        }
    }
}
