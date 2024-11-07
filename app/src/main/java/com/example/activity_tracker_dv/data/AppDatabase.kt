package com.example.activity_tracker_dv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.activity_tracker_dv.models.Event
import com.example.activity_tracker_dv.models.Followed
import com.example.activity_tracker_dv.utils.Converters

@Database(entities = [Event::class, Followed::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    abstract fun followedDao(): FollowedDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "activity_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
