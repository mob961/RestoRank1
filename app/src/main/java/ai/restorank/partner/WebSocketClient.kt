package ai.restorank.partner

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val url: String,
    private val listener: Listener
) {
    interface Listener {
        fun onOpen()
        fun onMessage(message: String)
        fun onClose(code: Int, reason: String)
        fun onError(error: Exception)
    }

    companion object {
        private const val TAG = "WebSocketClient"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var shouldReconnect = true

    fun connect() {
        shouldReconnect = true
        doConnect()
    }

    private fun doConnect() {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                mainHandler.post { listener.onOpen() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: ${text.take(100)}")
                mainHandler.post { listener.onMessage(text) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                mainHandler.post { listener.onClose(code, reason) }
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                mainHandler.post { listener.onError(Exception(t.message ?: "WebSocket error")) }
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (shouldReconnect) {
            mainHandler.postDelayed({
                if (shouldReconnect) {
                    Log.d(TAG, "Attempting to reconnect...")
                    doConnect()
                }
            }, RECONNECT_DELAY_MS)
        }
    }

    fun send(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    fun close() {
        shouldReconnect = false
        webSocket?.close(1000, "Client closing")
        webSocket = null
    }
}
