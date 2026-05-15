package com.example.railubuddy.services

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RailuAlarmService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "RAILU_SERVICE_CHANNEL"

        // This keeps the service from being killed by the system
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Namma Railu Buddy Active")
            .setContentText("Monitoring your location for station alarms...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}