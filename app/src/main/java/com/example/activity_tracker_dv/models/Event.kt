package com.example.activity_tracker_dv.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUsername: String = "",
    val eventType: String = "",
    var distanceTravelled: Double = 0.0,
    val launch: Date = Date(),
    var end: Date = Date(),
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    var endLatitude: Double = 0.0,
    var endLongitude: Double = 0.0
)
