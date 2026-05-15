package com.example.railubuddy.model

/**
 * Represents the full journey of a train.
 * Used for rendering the timeline and managing the overall trip status.
 */
data class TrainJourney(
    val train_no: Int,
    val train_name: String,
    val status: String,
    val stops: List<StationStop>
)

/**
 * Represents an individual stop along the route.
 * [lat] and [lng] are essential for the GPS proximity alerts.
 */
data class StationStop(
    val station_name: String,
    val station_code: String,
    val arrival: String,
    val departure: String,
    val distance_km: Int,
    val lat: Double, // Added for GPS distance calculations
    val lng: Double  // Added for GPS distance calculations
)