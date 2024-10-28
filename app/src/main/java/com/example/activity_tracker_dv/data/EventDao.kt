package com.example.activity_tracker_dv.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.activity_tracker_dv.models.Event

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: Event): Long

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Long): LiveData<Event>

    @Query("SELECT * FROM events ORDER BY launch DESC")
    fun getAllEvents(): LiveData<List<Event>>
}
