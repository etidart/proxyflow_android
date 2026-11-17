package io.github.etidart.proxyflow_android

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    private companion object {
        const val PROXY_LIST_FILENAME = "proxylist.txt"
        const val MAX_PROXY_LIST_SIZE = 10 * 1024 * 1024
        const val PREF_LAST_DOWNLOAD_URL = "last_download_url"
        const val PREF_LAST_DOWNLOAD_TIMESTAMP = "last_download_timestamp"
        const val PREF_URL_MODE = "url_mode"
    }

    private lateinit var statusText: TextView
    private lateinit var vpnButton: Button
    private lateinit var proxySourceButton: Button
    private lateinit var downloadButton: Button
    private lateinit var lastDownloadText: TextView

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
            updateUi(false)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestVpnPermission()
        } else {
            Toast.makeText(this, "Notification permission is required to keep VPN running", Toast.LENGTH_LONG).show()
        }
    }

    private val proxyFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            copyProxyFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        vpnButton = findViewById(R.id.vpnButton)
        proxySourceButton = findViewById(R.id.proxySourceButton)
        downloadButton = findViewById(R.id.downloadButton)
        lastDownloadText = findViewById(R.id.lastDownloadText)

        vpnButton.setOnClickListener { onVpnButtonClicked() }
        proxySourceButton.setOnClickListener { showProxySourceMenu() }
        downloadButton.setOnClickListener {
            val savedUrl = getPreferences(MODE_PRIVATE).getString(PREF_LAST_DOWNLOAD_URL, null)
            if (savedUrl != null) {
                downloadProxyList(savedUrl)
            }
        }

        updateLastDownloadUi()
        updateUi(PflowService.isRunning)
    }

    override fun onResume() {
        super.onResume()
        updateUi(PflowService.isRunning)
    }

    private fun onVpnButtonClicked() {
        if (PflowService.isRunning) {
            onDisconnectClicked()
        } else if (File(filesDir, PROXY_LIST_FILENAME).exists()) {
            continueWithVpnPermission()
        } else {
            showProxySourceMenu()
        }
    }

    private fun showProxySourceMenu() {
        AlertDialog.Builder(this)
            .setTitle(R.string.proxy_source_title)
            .setItems(R.array.proxy_source_choices) { _, which ->
                when (which) {
                    0 -> {
                        setUrlMode(false)
                        showManualProxyListDialog()
                    }
                    1 -> {
                        setUrlMode(false)
                        proxyFileLauncher.launch(arrayOf("text/*", "application/octet-stream"))
                    }
                    2 -> {
                        setUrlMode(true)
                        showProxyUrlDialog()
                    }
                }
            }
            .show()
    }

    private fun showManualProxyListDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.proxy_list_hint)
            minLines = 6
            gravity = android.view.Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.manual_proxy_list_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                saveProxyList(input.text.toString())
            }
            .show()
    }

    private fun showProxyUrlDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.proxy_url_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            getPreferences(MODE_PRIVATE).getString(PREF_LAST_DOWNLOAD_URL, null)?.let(::setText)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.proxy_url_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.download_label) { _, _ ->
                downloadProxyList(input.text.toString())
            }
            .show()
    }

    private fun copyProxyFile(uri: android.net.Uri) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { input ->
                readProxyList(input)
            } ?: throw IOException("Unable to open selected file")
            if (content.size > MAX_PROXY_LIST_SIZE) {
                throw IOException("Proxy list is too large")
            }
            saveProxyList(String(content, Charsets.UTF_8))
        } catch (error: IOException) {
            showError(error.message ?: getString(R.string.proxy_file_error))
        }
    }

    private fun downloadProxyList(rawUrl: String) {
        val url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            showError(getString(R.string.proxy_url_error))
            return
        }

        Toast.makeText(this, R.string.downloading_proxy_list, Toast.LENGTH_SHORT).show()
        thread(name = "proxy-list-download") {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.requestMethod = "GET"
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    throw IOException("HTTP ${connection.responseCode}")
                }
                val content = connection.inputStream.use { input ->
                    readProxyList(input)
                }
                connection.disconnect()
                if (content.size > MAX_PROXY_LIST_SIZE) {
                    throw IOException("Proxy list is too large")
                }
                runOnUiThread {
                    saveProxyList(String(content, Charsets.UTF_8), downloadedUrl = url)
                }
            } catch (error: IOException) {
                runOnUiThread { showError(error.message ?: getString(R.string.proxy_download_error)) }
            }
        }
    }

    private fun readProxyList(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) {
                return output.toByteArray()
            }
            output.write(buffer, 0, read)
            if (output.size() > MAX_PROXY_LIST_SIZE) {
                throw IOException("Proxy list is too large")
            }
        }
    }

    private fun saveProxyList(content: String, downloadedUrl: String? = null) {
        if (content.isBlank()) {
            showError(getString(R.string.proxy_list_empty))
            return
        }
        try {
            openFileOutput(PROXY_LIST_FILENAME, MODE_PRIVATE).use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            }
            if (downloadedUrl != null) {
                getPreferences(MODE_PRIVATE).edit {
                    putString(PREF_LAST_DOWNLOAD_URL, downloadedUrl)
                        .putLong(PREF_LAST_DOWNLOAD_TIMESTAMP, System.currentTimeMillis())
                }
                updateLastDownloadUi()
            }
            Toast.makeText(this, R.string.proxy_list_saved, Toast.LENGTH_SHORT).show()
        } catch (error: IOException) {
            showError(error.message ?: getString(R.string.proxy_file_error))
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateLastDownloadUi() {
        val preferences = getPreferences(MODE_PRIVATE)
        val savedUrl = preferences.getString(PREF_LAST_DOWNLOAD_URL, null)
        val timestamp = preferences.getLong(PREF_LAST_DOWNLOAD_TIMESTAMP, 0L)
        downloadButton.visibility = if (
            preferences.getBoolean(PREF_URL_MODE, false)
        ) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
        if (savedUrl == null || timestamp == 0L) {
            lastDownloadText.text = getString(R.string.last_download_never)
            return
        }

        val formattedTimestamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date(timestamp))
        lastDownloadText.text = getString(R.string.last_download_at, formattedTimestamp)
    }

    private fun setUrlMode(enabled: Boolean) {
        getPreferences(MODE_PRIVATE).edit {
            putBoolean(PREF_URL_MODE, enabled)
        }
        updateLastDownloadUi()
    }

    private fun continueWithVpnPermission() {
        // Android 13+ requires explicit POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        requestVpnPermission()
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // Show system dialog asking the user to allow VPN
            vpnPermissionLauncher.launch(intent)
        } else {
            // Permission already granted
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, PflowService::class.java)
        ContextCompat.startForegroundService(this, intent)
        updateUi(true)
    }

    private fun onDisconnectClicked() {
        stopService(Intent(this, PflowService::class.java))
        updateUi(false)
    }

    private fun updateUi(isRunning: Boolean) {
        if (isRunning) {
            statusText.text = getString(R.string.status_connected)
            vpnButton.text = getString(R.string.disconnect)
            proxySourceButton.isEnabled = false
            downloadButton.isEnabled = false
        } else {
            statusText.text = getString(R.string.status_disconnected)
            vpnButton.text = getString(R.string.connect)
            proxySourceButton.isEnabled = true
            downloadButton.isEnabled = true
        }
    }
}