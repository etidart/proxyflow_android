package io.github.etidart.proxyflow_android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import hev.sockstun.HevTunnel
import proxyflowwrapper.Proxyflowwrapper
import java.io.File

class PflowService : VpnService() {
    private var tunInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        Thread { startVpnStack() }.start()
        return START_STICKY
    }

    private fun isValidIpv4(candidate: String): Boolean {
        val parts = candidate.split('.')
        return parts.size == 4 && parts.all { part ->
            if (part.length > 1 && part[0] == '0') return@all false
            val n = part.toIntOrNull() ?: return@all false
            n in 0..255
        }
    }

    private fun startVpnStack() {
        val pfile = File(filesDir, PFILENAME)
        var builder = Builder()
            .setSession("Pflow")
            .setMtu(1500)
            .addAddress("10.8.0.2", 24)
            .addDnsServer("1.1.1.1")

        val ips = IP_REGEX.findAll(pfile.readText())
            .map { it.value }
            .filter(::isValidIpv4)
            .distinct()
            .toList()
        SubnetSplitter.coverExceptMany("0.0.0.0/0", ips)
            .forEach { builder = builder.addRoute(it.ip, it.prefix) }

        val proxyErr = Proxyflowwrapper.startSocks5("127.0.0.1:1080", pfile.absolutePath, 10)
        if (proxyErr.isNotEmpty()) {
            Log.e(TAG, "Proxy start failed: $proxyErr")
            cleanup()
            stopSelf()
            return
        }

        tunInterface = builder.establish() ?: run {
            Log.e(TAG, "Failed to establish TUN")
            cleanup()
            stopSelf()
            return
        }

        val cfg = File(filesDir, "hev-config.yaml")
        cfg.writeText(
            """
                tunnel:
                  mtu: 1500
                  ipv4: 10.8.0.2
                socks5:
                  port: 1080
                  address: 127.0.0.1
                  udp: 'udp'
            """.trimIndent()
        )

        HevTunnel.TProxyStartService(cfg.absolutePath, tunInterface!!.fd)
        isRunning = true
        Log.i(TAG, "VPN stack running")
    }

    private fun cleanup() {
        try {
            HevTunnel.TProxyStopService()
        } catch (_: Throwable) {}
        try {
            Proxyflowwrapper.stopSocks5()
        } catch (_: Throwable) {}
        try {
            tunInterface?.close()
        } catch (_: Exception) {}
        tunInterface = null
        isRunning = false
    }

    private fun startForegroundNotification() {
        val channelId = "vpn_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        // 1. Create the channel ONLY on API 26+.
        //    On API 24–25, this block is skipped, and the notification
        //    will still display correctly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pflow Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Build the notification with NotificationCompat.
        //    It accepts a channelId even on older APIs, where it is simply ignored.
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pflow Active")
            .setContentText("Your traffic is being routed securely.")
            .setSmallIcon(R.drawable.ic_vpn) // Replace with your icon
            .setPriority(NotificationCompat.PRIORITY_LOW) // For API < 26
            .build()

        // 3. startForeground() itself is available on all API levels.
        startForeground(1, notification)
    }

    override fun onDestroy() {
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PflowService"
        private val IP_REGEX = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
        const val PFILENAME = "proxylist.txt"
        @Volatile var isRunning = false
            private set
    }
}

