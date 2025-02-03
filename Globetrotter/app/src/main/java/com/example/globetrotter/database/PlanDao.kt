package com.example.globetrotter.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: Plan)

    @Update
    suspend fun updatePlan(plan: Plan)

    @Delete
    suspend fun deletePlan(plan: Plan)

    @Query("DELETE FROM plans")
    suspend fun deleteAll()

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlanById(id: Int): Plan

    @Query("SELECT * FROM plans WHERE itineraryId = :itineraryId")
    suspend fun getPlansByItinerary(itineraryId: Int): List<Plan>

}