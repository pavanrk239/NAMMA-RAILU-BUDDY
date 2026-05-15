package com.example.railubuddy.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RailuService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        // This is the notification that appears in the status bar
        // to tell Android: "Don't kill this app, I'm busy tracking a train!"
        val notification = NotificationCompat.Builder(this, "RAILU_SERVICE_CHANNEL")
            .setContentTitle("Namma Railu Buddy")
            .setContentText("Monitoring your journey to ensure you don't miss your stop.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "RAILU_SERVICE_CHANNEL",
                "Journey Monitor Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}