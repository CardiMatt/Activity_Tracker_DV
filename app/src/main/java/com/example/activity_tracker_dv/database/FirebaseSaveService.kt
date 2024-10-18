package com.example.activity_tracker_dv.database

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseSaveService : Service() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val activityType = intent?.getStringExtra("activityType") ?: ""
        val steps = intent?.getIntExtra("steps", 0) ?: 0
        val startTime = intent?.getLongExtra("startTime", 0L) ?: 0L
        val endTime = intent?.getLongExtra("endTime", 0L) ?: 0L

        // Salva l'attività su Firebase
        saveActivityToFirebase(activityType, steps, startTime, endTime)

        return START_NOT_STICKY
    }

    private fun saveActivityToFirebase(activityType: String, steps: Int, startTime: Long, endTime: Long) {
        val activityData = hashMapOf(
            "activityType" to activityType,
            "steps" to steps,
            "startTime" to startTime,
            "endTime" to endTime
        )

        firestore.collection("activities")
            .add(activityData)
            .addOnSuccessListener {
                stopSelf()
            }
            .addOnFailureListener {
                stopSelf()
            }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
