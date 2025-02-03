package com.example.globetrotter.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "plans",
    foreignKeys = [
        ForeignKey(
            entity = Itinerary::class,
            parentColumns = ["id"],
            childColumns = ["itineraryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Plan (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itineraryId: Int,
    val place: Place
)