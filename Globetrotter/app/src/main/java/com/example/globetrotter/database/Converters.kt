package com.example.globetrotter.database

import androidx.room.TypeConverter
import com.google.gson.Gson

// This was a suggestion from an error by Android Studio
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromPlace(place: Place): String {
        return gson.toJson(place)
    }

    @TypeConverter
    fun toPlace(json: String): Place {
        return gson.fromJson(json, Place::class.java)
    }
}