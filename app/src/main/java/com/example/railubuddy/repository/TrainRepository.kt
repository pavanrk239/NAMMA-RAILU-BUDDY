package com.example.railubuddy.repository // Use your actual package name

import android.content.Context
import com.example.railubuddy.model.TrainJourney
import com.google.gson.Gson
import java.io.IOException

class TrainRepository(private val context: Context) {

    fun getMockJourneyData(): TrainJourney? {
        return try {
            // 1. Open the file from assets
            val jsonString = context.assets.open("mock_route.json")
                .bufferedReader()
                .use { it.readText() }

            // 2. Parse JSON into our TrainJourney data class
            Gson().fromJson(jsonString, TrainJourney::class.java)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}