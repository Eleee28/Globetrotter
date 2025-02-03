package com.example.globetrotter.database

import androidx.room.*

@Dao
interface ItineraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itinerary: Itinerary)

    @Update
    suspend fun update(itinerary: Itinerary)

    @Delete
    suspend fun delete(itinerary: Itinerary)

    @Query("DELETE FROM itinerary")
    suspend fun deleteAll()

    @Query("SELECT * FROM itinerary WHERE id = :id")
    suspend fun getItineraryById(id: Int): Itinerary

    @Query("SELECT * FROM itinerary")
    suspend fun getAllItineraries(): List<Itinerary>
}