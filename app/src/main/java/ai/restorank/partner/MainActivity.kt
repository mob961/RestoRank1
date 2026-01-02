package ai.restorank.partner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var printerService: PrinterService
    private var webSocketClient: WebSocketClient? = null
    private val isConnected = AtomicBoolean(false)

    companion object {
        private const val WEB_URL = "https://66f7027a-5068-447b-989d-40beb0e60e83-00-2j1h3v377ow45.worf.replit.dev/#/dashboard/tables"
        private const val WS_URL = "wss://66f7027a-5068-447b-989d-40beb0e60e83-00-2j1h3v377ow45.worf.replit.dev/ws/orders"
        private const val RESTAURANT_ID = "5a7f3275-5f63-4d8e-83dc-b544540e79c3"
        private const val TAG = "RestoRankPartner"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        printerService = PrinterService()
        
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        webView = findViewById(R.id.webView)

        setupWebView()
        setupSwipeRefresh()
        connectWebSocket()

        webView.loadUrl(WEB_URL)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_printer -> {
                startActivity(Intent(this, PrinterSettingsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun connectWebSocket() {
        val wsUrl = "$WS_URL?restaurantId=$RESTAURANT_ID"
        
        webSocketClient = WebSocketClient(wsUrl, object : WebSocketClient.Listener {
            override fun onOpen() {
                isConnected.set(true)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connected to orders", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onMessage(message: String) {
                handleOrderEvent(message)
            }

            override fun onClose(code: Int, reason: String) {
                isConnected.set(false)
                android.util.Log.d(TAG, "WebSocket closed: $reason")
            }

            override fun onError(error: Exception) {
                android.util.Log.e(TAG, "WebSocket error: ${error.message}")
            }
        })
        
        webSocketClient?.connect()
    }

    private fun handleOrderEvent(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type")
            
            if (type == "order:new") {
                val prefs = getSharedPreferences(PrinterSettingsActivity.PREF_NAME, Context.MODE_PRIVATE)
                val autoPrint = prefs.getBoolean(PrinterSettingsActivity.KEY_AUTO_PRINT, true)
                
                if (autoPrint) {
                    val orderData = json.getJSONObject("data")
                    printOrder(orderData)
                }
                
                runOnUiThread {
                    Toast.makeText(this, "New order received!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error handling order event: ${e.message}")
        }
    }

    private fun printOrder(orderData: JSONObject) {
        val prefs = getSharedPreferences(PrinterSettingsActivity.PREF_NAME, Context.MODE_PRIVATE)
        val printerIp = prefs.getString(PrinterSettingsActivity.KEY_PRINTER_IP, "") ?: ""
        val printerPort = prefs.getString(PrinterSettingsActivity.KEY_PRINTER_PORT, "9100")?.toIntOrNull() ?: 9100
        
        if (printerIp.isEmpty()) {
            android.util.Log.w(TAG, "No printer configured")
            return
        }

        val orderId = orderData.optString("id", "")
        val orderType = orderData.optString("type", "dine-in")
        val tableId = orderData.optString("tableId", "")
        val itemsArray = orderData.optJSONArray("items") ?: JSONArray()
        
        val items = mutableListOf<PrinterService.OrderItem>()
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.getJSONObject(i)
            items.add(PrinterService.OrderItem(
                name = item.optString("name", ""),
                quantity = item.optInt("quantity", 1),
                price = item.optString("price", "0"),
                options = null
            ))
        }

        val ticketContent = printerService.formatKitchenTicket(
            orderId = orderId,
            orderType = orderType,
            tableName = tableId.takeLast(4),
            items = items
        )

        lifecycleScope.launch {
            val result = printerService.printRaw(printerIp, printerPort, ticketContent)
            result.fold(
                onSuccess = {
                    android.util.Log.d(TAG, "Order printed successfully")
                },
                onFailure = { e ->
                    android.util.Log.e(TAG, "Print failed: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Print failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        this@MainActivity,
                        "Connection error. Pull down to refresh.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (url.contains("restorank") || url.contains("replit.dev")) {
                        false
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        true
                    }
                } else {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Cannot open this link", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                android.util.Log.d(TAG, "WebView: ${consoleMessage?.message()}")
                return true
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )

        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        if (!isConnected.get()) {
            connectWebSocket()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webSocketClient?.close()
        webView.destroy()
        super.onDestroy()
    }
}
