package io.github.etidart.proxyflow_android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.InetSocketAddress

class PflowService : VpnService() {
    private var tunInterface: ParcelFileDescriptor? = null
    companion object {
        @Volatile var isRunning = false
            private set
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        val builder = Builder()
        builder.setSession("Pflow")
        builder.addAddress("10.8.0.2", 24)
        builder.addDnsServer("8.8.8.8")
        builder.addRoute("0.0.0.0", 0)

        tunInterface = builder.establish()
        isRunning = true

        Thread { vpnLoop() }.start()
    }

    private fun vpnLoop() {
        val fd = tunInterface?.fileDescriptor ?: return
        val tunIn = FileInputStream(fd)
        val tunOut = FileOutputStream(fd)
        val socket = DatagramSocket()
        protect(socket)

        val server = InetSocketAddress("asdasd", 5555)
        val buffer = ByteArray(32767)

        while (isRunning) {
            val length = tunIn.read(buffer)
            if (length > 0) {
                socket.send(java.net.DatagramPacket(buffer, length, server))
                try {
                    val response = java.net.DatagramPacket(buffer, buffer.size)
                    socket.receive(response)
                    tunOut.write(buffer, 0, response.length)
                } catch (_: Exception) { /* handle error */ }
            }
        }
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
        isRunning = false
        tunInterface?.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

