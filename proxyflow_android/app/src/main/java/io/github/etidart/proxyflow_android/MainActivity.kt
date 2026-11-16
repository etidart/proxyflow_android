package io.github.etidart.proxyflow_android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)

        connectButton.setOnClickListener { onConnectClicked() }
        disconnectButton.setOnClickListener { onDisconnectClicked() }

        updateUi(PflowService.isRunning)
    }

    override fun onResume() {
        super.onResume()
        updateUi(PflowService.isRunning)
    }

    private fun onConnectClicked() {
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
            connectButton.isEnabled = false
            disconnectButton.isEnabled = true
        } else {
            statusText.text = getString(R.string.status_disconnected)
            connectButton.isEnabled = true
            disconnectButton.isEnabled = false
        }
    }
}