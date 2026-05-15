package com.example.railubuddy.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Base URL for the train API service
    private const val BASE_URL = "https://indian-railway-api.p.rapidapi.com/"

    val instance: TrainApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Converts JSON to Kotlin objects
            .build()

        retrofit.create(TrainApiService::class.java)
    }
}