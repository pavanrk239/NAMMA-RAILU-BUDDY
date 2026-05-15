package com.example.railubuddy

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.railubuddy.screens.HomeScreen
import com.example.railubuddy.screens.JourneyScreen
import com.example.railubuddy.screens.StationScreen
import com.example.railubuddy.viewmodel.MainViewModel
import com.google.android.gms.location.*

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val CHANNEL_ID = "railu_buddy_alarms"
    private val STOP_ACTION = "STOP_ALARM_ACTION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = viewModel()

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                        startLocationUpdates(viewModel)
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.POST_NOTIFICATIONS
                            } else {
                                Manifest.permission.ACCESS_FINE_LOCATION
                            }
                        )
                    )
                }

                val alarmTarget by viewModel.liveProximityAlarm.collectAsStateWithLifecycle(initialValue = null)

                LaunchedEffect(alarmTarget) {
                    alarmTarget?.let { station ->
                        triggerVibration()
                        showOnScreenNotification(station.name)
                        Toast.makeText(this@MainActivity, "🚨 Arriving at ${station.name}!", Toast.LENGTH_LONG).show()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController, viewModel) }
                        composable(
                            route = "journey_screen/{trainNumber}",
                            arguments = listOf(navArgument("trainNumber") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val trainNo = backStackEntry.arguments?.getString("trainNumber") ?: ""
                            JourneyScreen(navController, viewModel, trainNo)
                        }
                        composable(
                            route = "station_screen/{stationId}",
                            arguments = listOf(navArgument("stationId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("stationId") ?: ""
                            LaunchedEffect(id) { viewModel.selectStation(id) }
                            StationScreen(navController, id, viewModel)
                        }
                    }
                }
            }
        }
    }

    // Handles the "STOP" button click from the notification
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == STOP_ACTION) {
            stopVibration()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(1) // Remove the notification after stopping
        }
    }

    private fun stopVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Station Alarms"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Notifications for arriving at stations"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showOnScreenNotification(stationName: String) {
        // Intent that points back to this Activity
        val stopIntent = Intent(this, MainActivity::class.java).apply {
            action = STOP_ACTION
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val stopPendingIntent = PendingIntent.getActivity(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Arriving at $stationName")
            .setContentText("Almost there! Tap STOP to silence alarm.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            // Adds the STOP Button to the notification banner
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP ALARM", stopPendingIntent)
            .setFullScreenIntent(stopPendingIntent, true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    private fun triggerVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timing = longArrayOf(0, 500, 300, 500)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timing, amplitudes, 0)) // 0 = Loop
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 300, 500), 0)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(viewModel: MainViewModel) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location -> viewModel.updateLocation(location) }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }
}