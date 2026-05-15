package com.example.railubuddy.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Fixed: Renamed to camelCase to resolve IDE naming convention warning
    private val channelId = "STATION_ALARM_CHANNEL"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        // Handle the STOP_ALARM action sent from the notification button
        if (intent.action == "STOP_ALARM") {
            stopVibrationAndKillService()
            return START_NOT_STICKY
        }

        val stationName = intent.getStringExtra("STATION_NAME") ?: "Station"
        val destLat = intent.getDoubleExtra("LAT", 0.0)
        val destLon = intent.getDoubleExtra("LON", 0.0)

        // Android 14+ Requirement: Explicitly declare foregroundServiceType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                createPersistentNotification(stationName),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(1, createPersistentNotification(stationName))
        }

        monitorProximity(stationName, destLat, destLon)

        return START_STICKY
    }

    private fun monitorProximity(name: String, lat: Double, lon: Double) {
        serviceScope.launch {
            while (isActive) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    this@LocationService,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    try {
                        val location: Location? = fusedLocationClient.lastLocation.await()
                        location?.let {
                            val results = FloatArray(1)
                            Location.distanceBetween(it.latitude, it.longitude, lat, lon, results)

                            // 5000f = 5km threshold for arrival
                            if (results[0] <= 5000f) {
                                sendArrivalNotification(name)
                                cancel()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(15000) // Check every 15 seconds to balance accuracy and battery
            }
        }
    }

    private fun sendArrivalNotification(stationName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification action to stop the alarm
        val stopIntent = Intent(this, LocationService::class.java).apply { action = "STOP_ALARM" }
        val stopPendingIntent = PendingIntent.getService(
            this, 101, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        triggerVibration()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Arriving at $stationName")
            .setContentText("You are within 5KM. Prepare to deboard!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP ALARM", stopPendingIntent)
            .build()

        manager.notify(2, notification)
    }

    private fun triggerVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun createPersistentNotification(stationName: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Namma Railu Buddy")
            .setContentText("Monitoring distance to $stationName...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Station Proximity Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when close to destination"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopVibrationAndKillService() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(2)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?) = null
}