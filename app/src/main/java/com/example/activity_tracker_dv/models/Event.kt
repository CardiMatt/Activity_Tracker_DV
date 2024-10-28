package com.example.activity_tracker_dv.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUsername: String,
    val eventType: String,
    var steps: Int,
    val launch: Date,
    var end: Date,
    val startLatitude: Double,
    val startLongitude: Double,
    var endLatitude: Double,
    var endLongitude: Double
)
