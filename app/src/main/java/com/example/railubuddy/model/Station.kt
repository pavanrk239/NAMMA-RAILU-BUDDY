package com.example.railubuddy.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Station(
    val stationId: String = "",
    val name: String = "",
    val trainName: String = "",
    val trainNumber: String = "",
    val routeOrder: Int = 0,
    val origin: String = "",
    val destination: String = "",
    val platform: String = "1",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val scheduledArrival: String = "--:--",
    val actualArrival: String = "--:--",
    val delay: Int = 0,
    val votes: Int = 0,
    val coaches: Map<String, Map<String, Int>> = emptyMap()
)