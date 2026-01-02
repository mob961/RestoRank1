package ai.restorank.partner

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PrinterSettingsActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    private lateinit var printerService: PrinterService
    
    private lateinit var etPrinterIp: EditText
    private lateinit var etPrinterPort: EditText
    private lateinit var etPrinterName: EditText
    private lateinit var switchAutoPrint: Switch
    private lateinit var btnTestConnection: Button
    private lateinit var btnTestPrint: Button
    private lateinit var btnSave: Button
    private lateinit var tvStatus: TextView

    companion object {
        const val PREF_NAME = "printer_settings"
        const val KEY_PRINTER_IP = "printer_ip"
        const val KEY_PRINTER_PORT = "printer_port"
        const val KEY_PRINTER_NAME = "printer_name"
        const val KEY_AUTO_PRINT = "auto_print"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_settings)

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        printerService = PrinterService()

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        etPrinterIp = findViewById(R.id.etPrinterIp)
        etPrinterPort = findViewById(R.id.etPrinterPort)
        etPrinterName = findViewById(R.id.etPrinterName)
        switchAutoPrint = findViewById(R.id.switchAutoPrint)
        btnTestConnection = findViewById(R.id.btnTestConnection)
        btnTestPrint = findViewById(R.id.btnTestPrint)
        btnSave = findViewById(R.id.btnSave)
        tvStatus = findViewById(R.id.tvStatus)

        supportActionBar?.apply {
            title = "Printer Settings"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun loadSettings() {
        etPrinterIp.setText(prefs.getString(KEY_PRINTER_IP, "192.168.68.50"))
        etPrinterPort.setText(prefs.getString(KEY_PRINTER_PORT, "9100"))
        etPrinterName.setText(prefs.getString(KEY_PRINTER_NAME, "Kitchen"))
        switchAutoPrint.isChecked = prefs.getBoolean(KEY_AUTO_PRINT, true)
    }

    private fun setupListeners() {
        btnTestConnection.setOnClickListener {
            testConnection()
        }

        btnTestPrint.setOnClickListener {
            testPrint()
        }

        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun testConnection() {
        val ip = etPrinterIp.text.toString().trim()
        val port = etPrinterPort.text.toString().toIntOrNull() ?: 9100

        if (ip.isEmpty()) {
            showStatus("Please enter printer IP address", false)
            return
        }

        btnTestConnection.isEnabled = false
        showStatus("Testing connection...", null)

        lifecycleScope.launch {
            val result = printerService.testConnection(ip, port)
            btnTestConnection.isEnabled = true
            
            result.fold(
                onSuccess = {
                    showStatus("Connection successful!", true)
                },
                onFailure = { e ->
                    showStatus("Connection failed: ${e.message}", false)
                }
            )
        }
    }

    private fun testPrint() {
        val ip = etPrinterIp.text.toString().trim()
        val port = etPrinterPort.text.toString().toIntOrNull() ?: 9100

        if (ip.isEmpty()) {
            showStatus("Please enter printer IP address", false)
            return
        }

        btnTestPrint.isEnabled = false
        showStatus("Sending test print...", null)

        lifecycleScope.launch {
            val result = printerService.printTestPage(ip, port)
            btnTestPrint.isEnabled = true
            
            result.fold(
                onSuccess = {
                    showStatus("Test print sent successfully!", true)
                },
                onFailure = { e ->
                    showStatus("Print failed: ${e.message}", false)
                }
            )
        }
    }

    private fun saveSettings() {
        prefs.edit().apply {
            putString(KEY_PRINTER_IP, etPrinterIp.text.toString().trim())
            putString(KEY_PRINTER_PORT, etPrinterPort.text.toString().trim())
            putString(KEY_PRINTER_NAME, etPrinterName.text.toString().trim())
            putBoolean(KEY_AUTO_PRINT, switchAutoPrint.isChecked)
            apply()
        }
        
        showStatus("Settings saved!", true)
        Toast.makeText(this, "Printer settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun showStatus(message: String, success: Boolean?) {
        tvStatus.text = message
        tvStatus.setTextColor(
            when (success) {
                true -> getColor(android.R.color.holo_green_dark)
                false -> getColor(android.R.color.holo_red_dark)
                null -> getColor(android.R.color.darker_gray)
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
