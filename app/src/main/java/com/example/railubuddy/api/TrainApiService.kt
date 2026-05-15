package com.example.railubuddy.api

import com.example.railubuddy.model.Station
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TrainApiService {

    @GET("train/schedule")
    suspend fun getTrainSchedule(
        @Query("trainNo") trainNumber: String,
        @Header("X-RapidAPI-Key") apiKey: String = "YOUR_API_KEY_HERE",
        @Header("X-RapidAPI-Host") apiHost: String = "indian-railway-api.p.rapidapi.com"
    ): Response<List<Station>>

    @GET("train/liveStatus")
    suspend fun getLiveStatus(
        @Query("trainNo") trainNumber: String,
        @Header("X-RapidAPI-Key") apiKey: String = "YOUR_API_KEY_HERE"
    ): Response<LiveStatusResponse>
}

data class LiveStatusResponse(
    val currentStation: String,
    val delayInMinutes: Int,
    val lastUpdated: String
)