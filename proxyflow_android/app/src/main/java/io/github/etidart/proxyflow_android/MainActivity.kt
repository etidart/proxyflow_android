package io.github.etidart.proxyflow_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import io.github.etidart.proxyflow_android.ui.MainScreen
import io.github.etidart.proxyflow_android.ui.theme.Proxyflow_androidTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private companion object {
        const val MAX_PROXY_LIST_SIZE = 10 * 1024 * 1024
    }

    private lateinit var prefs: Prefs

    private var selectedMode by mutableStateOf(SourceMode.MANUAL)
    private var manualText by mutableStateOf("")
    private var lastDownloadUrl by mutableStateOf<String?>(null)
    private var lastDownloadTimestamp by mutableStateOf(0L)
    private var isDownloading by mutableStateOf(false)
    private var isConnected by mutableStateOf(false)

    private var showManualDialog by mutableStateOf(false)
    private var showUrlDialog by mutableStateOf(false)
    private var submitManualRequested by mutableStateOf(false)
    private var submitUrlRequested by mutableStateOf(false)

    private var manualDraft by mutableStateOf("")
    private var urlDraft by mutableStateOf("")

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            showError(getString(R.string.vpn_permission_denied))
            refreshConnectionState()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestVpnPermission()
        } else {
            showError(getString(R.string.notification_permission_required))
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
        enableEdgeToEdge()
        prefs = Prefs(this)
        restoreState()

        setContent {
            Proxyflow_androidTheme {
                MainScreen(
                    selectedMode = selectedMode,
                    manualText = manualText,
                    lastDownloadUrl = lastDownloadUrl,
                    lastDownloadTimestamp = lastDownloadTimestamp,
                    isConnected = isConnected,
                    isDownloading = isDownloading,
                    showManualDialog = showManualDialog,
                    showUrlDialog = showUrlDialog,
                    submitManual = submitManualRequested,
                    submitUrl = submitUrlRequested,
                    manualDraft = manualDraft,
                    urlDraft = urlDraft,
                    onManualDraftChange = { manualDraft = it },
                    onUrlDraftChange = { urlDraft = it },
                    onSelectMode = ::onModeSelected,
                    onConnectToggle = ::onConnectToggle,
                    onDismissManualDialog = { showManualDialog = false },
                    onSubmitManual = ::onSubmitManual,
                    onDismissUrlDialog = { showUrlDialog = false },
                    onSubmitUrl = ::onSubmitUrl,
                    onManualSubmitted = ::onManualSubmitted,
                    onUrlSubmitted = ::onUrlSubmitted,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshConnectionState()
    }

    private fun restoreState() {
        selectedMode = prefs.sourceMode
        manualText = prefs.manualText
        lastDownloadUrl = prefs.lastDownloadUrl
        lastDownloadTimestamp = prefs.lastDownloadTimestamp
        isConnected = PflowService.isRunning
    }

    private fun refreshConnectionState() {
        isConnected = PflowService.isRunning
    }

    // region Selection

    private fun onModeSelected(mode: SourceMode) {
        if (isConnected) {
            return
        }
        selectedMode = mode
        prefs.sourceMode = mode
        when (mode) {
            SourceMode.MANUAL -> {
                submitManualRequested = false
                manualDraft = prefs.manualText
                showManualDialog = true
            }
            SourceMode.FILE -> proxyFileLauncher.launch(arrayOf("text/*", "application/octet-stream"))
            SourceMode.URL -> {
                submitUrlRequested = false
                urlDraft = prefs.lastDownloadUrl.orEmpty()
                showUrlDialog = true
            }
        }
    }

    private fun onSubmitManual() {
        submitManualRequested = true
    }

    private fun onManualSubmitted(content: String) {
        submitManualRequested = false
        if (content.isBlank()) {
            showError(getString(R.string.proxy_list_empty))
            return
        }
        showManualDialog = false
        manualText = content
        prefs.manualText = content
        saveProxyList(content, downloadedUrl = null, fromManual = true)
    }

    private fun onSubmitUrl() {
        submitUrlRequested = true
    }

    private fun onUrlSubmitted(url: String) {
        submitUrlRequested = false
        downloadProxyList(url)
    }

    // endregion

    // region Proxy files

    private fun copyProxyFile(uri: android.net.Uri) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { input ->
                readProxyList(input)
            } ?: throw IOException(getString(R.string.proxy_file_error))
            saveProxyList(String(content, Charsets.UTF_8), downloadedUrl = null, fromManual = false)
        } catch (error: IOException) {
            showError(error.message ?: getString(R.string.proxy_file_error))
        }
    }

    private fun downloadProxyList(rawUrl: String) {
        if (isDownloading) return
        val url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            showError(getString(R.string.proxy_url_error))
            return
        }

        isDownloading = true
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
                val text = String(content, Charsets.UTF_8)
                runOnUiThread {
                    isDownloading = false
                    showUrlDialog = false
                    saveProxyList(text, downloadedUrl = url, fromManual = false)
                }
            } catch (error: IOException) {
                runOnUiThread {
                    isDownloading = false
                    showUrlDialog = false
                    showError(error.message ?: getString(R.string.proxy_download_error))
                }
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

    private fun saveProxyList(
        content: String,
        downloadedUrl: String?,
        fromManual: Boolean,
    ) {
        if (content.isBlank()) {
            showError(getString(R.string.proxy_list_empty))
            return
        }
        try {
            openFileOutput(PflowService.PFILENAME, MODE_PRIVATE).use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            }
            if (fromManual) {
                manualText = content
                prefs.manualText = content
            }
            if (downloadedUrl != null) {
                lastDownloadUrl = downloadedUrl
                lastDownloadTimestamp = System.currentTimeMillis()
                prefs.lastDownloadUrl = downloadedUrl
                prefs.lastDownloadTimestamp = lastDownloadTimestamp
            }
            showMessage(getString(R.string.proxy_list_saved))
        } catch (error: IOException) {
            showError(error.message ?: getString(R.string.proxy_file_error))
        }
    }

    // endregion

    // region VPN

    private fun onConnectToggle() {
        if (isConnected) {
            stopService(Intent(this, PflowService::class.java))
            isConnected = false
            return
        }
        if (!File(filesDir, PflowService.PFILENAME).exists()) {
            onModeSelected(selectedMode)
            showError(getString(R.string.choose_source_first))
            return
        }
        continueWithVpnPermission()
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
        isConnected = true
    }

    // endregion

    // region Feedback

    private fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    // endregion
}
