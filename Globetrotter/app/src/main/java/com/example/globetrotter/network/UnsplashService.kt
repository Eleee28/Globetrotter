package com.example.globetrotter.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// From Chat-GPT to be able to use Unsplash API
interface UnsplashService {

    @GET("search/photos")
    suspend fun searchImages(
        @Query("query") query: String,
        @Header("Authorization") apiKey: String,
        @Query("per_page") perPage: Int = 1
    ): UnsplashResponse
}
// End reference
