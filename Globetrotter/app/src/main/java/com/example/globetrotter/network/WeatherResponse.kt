package com.example.globetrotter.network

data class WeatherResponse(
    val list: List<WeatherItem>,
    val city: City
)

data class WeatherItem(
    val dt_text: String,        // Date and time
    val main: Main,             // Main weather data
    val weather: List<Weather>  // Weather details
)

data class Main(
    val temp: Float,
    val feels_like: Float,
    val humidity: Int
)

data class Weather(
    val description: String,
    val icon: String
)