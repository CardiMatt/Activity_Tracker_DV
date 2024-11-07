package com.example.activity_tracker_dv.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed")
data class Followed(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user: String,
    val followed: String
)
