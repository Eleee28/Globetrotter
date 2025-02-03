package com.example.globetrotter.network

data class GeoResponse(val data: List<City>)

data class City(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)
