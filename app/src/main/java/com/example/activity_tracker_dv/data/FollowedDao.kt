package com.example.activity_tracker_dv.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.activity_tracker_dv.models.Followed

@Dao
interface FollowedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowed(followed: Followed)

    @Delete
    suspend fun deleteFollowed(followed: Followed)

    @Query("DELETE FROM followed WHERE user = :userEmail")
    suspend fun deleteAllFollowedForUser(userEmail: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedList(followedList: List<Followed>)

    @Query("SELECT * FROM followed WHERE user = :userEmail")
    fun getFollowedUsers(userEmail: String): LiveData<List<Followed>>
}
