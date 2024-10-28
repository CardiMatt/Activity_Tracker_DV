package com.example.activity_tracker_dv.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.activity_tracker_dv.models.Event

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: Event): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<Event>)

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)

    @Delete
    suspend fun deleteAll(events: List<Event>)

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Long): LiveData<Event>

    @Query("SELECT * FROM events ORDER BY launch DESC")
    fun getAllEvents(): LiveData<List<Event>>

    @Query("SELECT * FROM events ORDER BY launch DESC")
    suspend fun getAllEventsList(): List<Event>

    @Query("SELECT * FROM events WHERE userUsername = :userUsername ORDER BY launch DESC")
    fun getEventsByUser(userUsername: String): LiveData<List<Event>>
}
