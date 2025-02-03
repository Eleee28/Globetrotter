package com.example.globetrotter.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// From Chat-GPT, Retrofit instance to make API calls
object RetrofitClient {
    private const val UNSPLASH_BASE_URL = "https://api.unsplash.com/"
    private const val WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private const val GEODB_BASE_URL = "https://wft-geo-db.p.rapidapi.com/v1/geo/"

    // OkHttpClient with Gzip Decompression
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // Unsplash service
    val unsplashService: UnsplashService by lazy {
        Retrofit.Builder()
            .baseUrl(UNSPLASH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient) // Gzip support via okHttpClient
            .build()
            .create(UnsplashService::class.java)
    }
    // End reference

    // Weather API service
    val weatherService: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient) // Gzip support via okHttpClient
            .build()
            .create(WeatherApiService::class.java)
    }

    // GeoCities API service
    val geoService: GeoService by lazy {
        Retrofit.Builder()
            .baseUrl(GEODB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient) // Gzip support via okHttpClient
            .build()
            .create(GeoService::class.java)
    }

}