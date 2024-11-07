package com.example.activity_tracker_dv.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.activity_tracker_dv.data.FollowedDao
import com.example.activity_tracker_dv.models.Followed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FollowedRepository(private val followedDao: FollowedDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private val COLLECTION_NAME = "followed"

    suspend fun followUser(followedEmail: String): Result<Unit> {
        val currentUserEmail = auth.currentUser?.email ?: return Result.failure(Exception("Utente non autenticato"))

        try {
            // Controlla se esistono eventi associati all'utente da seguire
            val eventsSnapshot = firestore.collection("events")
                .whereEqualTo("userUsername", followedEmail)
                .get()
                .await()

            if (eventsSnapshot.isEmpty) {
                return Result.failure(Exception("L'utente da seguire non esiste o non ha eventi registrati"))
            }

            val followed = Followed(user = currentUserEmail, followed = followedEmail)

            firestore.collection(COLLECTION_NAME)
                .add(followed)
                .await()

            followedDao.insertFollowed(followed)
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FollowedRepository", "Error following user", e)
            return Result.failure(e)
        }
    }

    suspend fun unfollowUser(followedEmail: String): Result<Unit> {
        val currentUserEmail = auth.currentUser?.email ?: return Result.failure(Exception("Utente non autenticato"))

        return try {
            val querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("user", currentUserEmail)
                .whereEqualTo("followed", followedEmail)
                .get()
                .await()

            for (document in querySnapshot.documents) {
                firestore.collection(COLLECTION_NAME).document(document.id).delete().await()
            }

            val followed = Followed(user = currentUserEmail, followed = followedEmail)
            followedDao.deleteFollowed(followed)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FollowedRepository", "Error unfollowing user", e)
            Result.failure(e)
        }
    }

    suspend fun synchronizeFollowed() {
        val currentUserEmail = auth.currentUser?.email ?: return

        try {
            val firestoreFollowed = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("user", currentUserEmail)
                .get()
                .await()
                .toObjects(Followed::class.java)

            followedDao.deleteAllFollowedForUser(currentUserEmail)
            followedDao.insertFollowedList(firestoreFollowed)
        } catch (e: Exception) {
            Log.e("FollowedRepository", "Error synchronizing followed", e)
        }
    }

    fun getFollowedUsers(): LiveData<List<Followed>> {
        val currentUserEmail = auth.currentUser?.email ?: ""
        return followedDao.getFollowedUsers(currentUserEmail)
    }
}
