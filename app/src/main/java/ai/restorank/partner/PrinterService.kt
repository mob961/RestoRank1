package ai.restorank.partner

import android.util.Log
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class PrinterService {
    companion object {
        private const val TAG = "PrinterService"
        private const val CONNECTION_TIMEOUT = 5000
        private const val DEFAULT_PORT = 9100
    }

    private val ESC = '\u001B'
    private val GS = '\u001D'
    private val LF = '\n'

    private val INIT = "${ESC}@"
    private val BOLD_ON = "${ESC}E\u0001"
    private val BOLD_OFF = "${ESC}E\u0000"
    private val DOUBLE_SIZE = "${GS}!\u0030"
    private val NORMAL_SIZE = "${GS}!\u0000"
    private val ALIGN_LEFT = "${ESC}a\u0000"
    private val ALIGN_CENTER = "${ESC}a\u0001"
    private val CUT_PARTIAL = "${GS}V\u0001"
    private val CASH_DRAWER = "${ESC}p\u0000\u0019\u00FA"

    suspend fun testConnection(ip: String, port: Int = DEFAULT_PORT): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), CONNECTION_TIMEOUT)
            socket.close()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun printRaw(ip: String, port: Int, content: ByteArray, kickCashDrawer: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        var output: OutputStream? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), CONNECTION_TIMEOUT)
            output = socket.getOutputStream()
            
            output.write(content)
            
            if (kickCashDrawer) {
                output.write(CASH_DRAWER.toByteArray(Charsets.ISO_8859_1))
            }
            
            output.flush()
            delay(300)
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Print failed: ${e.message}")
            Result.failure(e)
        } finally {
            try {
                output?.close()
                socket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket: ${e.message}")
            }
        }
    }

    suspend fun printTestPage(ip: String, port: Int = DEFAULT_PORT): Result<Boolean> {
        val content = buildString {
            append(INIT)
            append(ALIGN_CENTER)
            append(DOUBLE_SIZE)
            append("TEST PRINT$LF")
            append(NORMAL_SIZE)
            append("================================$LF")
            append(ALIGN_LEFT)
            append("Printer IP: $ip:$port$LF")
            append("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}$LF")
            append("================================$LF")
            append(ALIGN_CENTER)
            append("RestoRank Partner App$LF")
            append("$LF$LF$LF")
            append(CUT_PARTIAL)
        }
        
        return printRaw(ip, port, content.toByteArray(Charsets.ISO_8859_1))
    }

    fun formatKitchenTicket(
        orderId: String,
        orderType: String,
        tableName: String,
        items: List<OrderItem>
    ): ByteArray {
        val content = buildString {
            append(INIT)
            append(ALIGN_CENTER)
            append(DOUBLE_SIZE)
            append("ORDER #${orderId.takeLast(4).uppercase()}$LF")
            append(NORMAL_SIZE)
            append(BOLD_ON)
            if (orderType == "dine-in") {
                append("TABLE $tableName$LF")
            } else {
                append("${orderType.uppercase()}$LF")
            }
            append(BOLD_OFF)
            append("================================$LF")
            append(ALIGN_LEFT)
            append("Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}$LF")
            append("--------------------------------$LF")
            
            items.forEach { item ->
                append(BOLD_ON)
                append("${item.quantity}x ${item.name}$LF")
                append(BOLD_OFF)
                item.options?.forEach { opt ->
                    append("   - $opt$LF")
                }
            }
            
            append("================================$LF")
            append("$LF$LF$LF")
            append(CUT_PARTIAL)
        }
        
        return content.toByteArray(Charsets.ISO_8859_1)
    }

    fun formatBillReceipt(
        orderId: String,
        orderType: String,
        tableName: String,
        items: List<OrderItem>,
        total: String
    ): ByteArray {
        val content = buildString {
            append(INIT)
            append(ALIGN_CENTER)
            append(DOUBLE_SIZE)
            append("BILL$LF")
            append(NORMAL_SIZE)
            append("Order #${orderId.takeLast(4).uppercase()}$LF")
            if (orderType == "dine-in") {
                append("Table $tableName$LF")
            } else {
                append("${orderType.uppercase()}$LF")
            }
            append("================================$LF")
            append(ALIGN_LEFT)
            
            items.forEach { item ->
                append("${item.quantity}x ${item.name}$LF")
                val itemTotal = (item.price.toDoubleOrNull() ?: 0.0) * item.quantity
                append("${" ".repeat(20)}$${String.format("%.2f", itemTotal)}$LF")
            }
            
            append("--------------------------------$LF")
            append(BOLD_ON)
            append("TOTAL: $$total$LF")
            append(BOLD_OFF)
            append("================================$LF")
            append(ALIGN_CENTER)
            append("Thank you for dining with us!$LF")
            append("$LF$LF$LF")
            append(CUT_PARTIAL)
        }
        
        return content.toByteArray(Charsets.ISO_8859_1)
    }

    data class OrderItem(
        val name: String,
        val quantity: Int,
        val price: String,
        val options: List<String>? = null
    )
}
