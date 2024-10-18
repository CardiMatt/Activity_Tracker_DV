package com.example.activity_tracker_dv.activity

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.activity_tracker_dv.database.FirebaseSaveService
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class ActivityRecognitionBackgroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (ActivityRecognitionResult.hasResult(it)) {
                val result = ActivityRecognitionResult.extractResult(it)
                val detectedActivity = result?.mostProbableActivity
                detectedActivity?.let { it1 -> handleDetectedActivity(it1) }
            }
        }
        return START_STICKY
    }

    private fun handleDetectedActivity(detectedActivity: DetectedActivity) {
        val newActivityType = getActivityTypeString(detectedActivity.type)

        if (newActivityType != ActivityStateManager.currentActivity.value) {
            // Salvare l'attività precedente su Firebase e aggiornare lo stato
            val previousActivity = ActivityStateManager.currentActivity.value ?: ""
            if (previousActivity.isNotEmpty()) {
                saveActivityToFirebase()
            }

            // Inizia una nuova attività
            ActivityStateManager.updateActivity(
                activity = newActivityType,
                steps = ActivityStateManager.currentSteps.value ?: 0,
                startTime = System.currentTimeMillis()
            )

            Log.d("ActivityRecognitionService", "New activity detected: $newActivityType")
        }
    }

    private fun saveActivityToFirebase() {
        val previousActivity = ActivityStateManager.currentActivity.value ?: ""
        val steps = ActivityStateManager.currentSteps.value ?: 0
        val startTime = ActivityStateManager.activityStartTime.value ?: 0L
        val endTime = System.currentTimeMillis()

        val firebaseServiceIntent = Intent(this, FirebaseSaveService::class.java).apply {
            putExtra("activityType", previousActivity)
            putExtra("steps", steps)
            putExtra("startTime", startTime)
            putExtra("endTime", endTime)
        }
        startService(firebaseServiceIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getActivityTypeString(type: Int): String {
        return when (type) {
            DetectedActivity.WALKING -> "walk"
            DetectedActivity.RUNNING -> "run"
            DetectedActivity.STILL -> "still"
            else -> "unknown"
        }
    }
}
