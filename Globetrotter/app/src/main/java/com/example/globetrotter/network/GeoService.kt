package com.example.globetrotter.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// options for query from: http://geodb-cities-api.wirefreethought.com/demo
interface GeoService {

    @GET("places")
    suspend fun getCities(
        @Query("namePrefix") namePrefix: String,
        @Query("limit") limit: Int = 5,
        @Query("offset") offset: Int = 0,
        @Query("sort") sort: String = "-population",
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") host: String = "wft-geo-db.p.rapidapi.com"
    ): GeoResponse
}