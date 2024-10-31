package com.example.activity_tracker_dv.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.activity_tracker_dv.R

class ActivityTrackingService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("ActivityTrackingService", "Service creato")
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ActivityTrackingService", "Avvio del monitoraggio delle attività e della posizione")

        // Avvia il monitoraggio delle attività utilizzando il BroadcastReceiver
        EventRecognitionReceiver.startActivityTransitionUpdates(applicationContext)

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoraggio attività in corso")
            .setContentText("L'app sta monitorando le tue attività fisiche.")
            //.setSmallIcon(R.drawable.ic_tracking)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ActivityTrackingService", "Service distrutto, fermo il monitoraggio")
        EventRecognitionReceiver.stopActivityTransitionUpdates(applicationContext)
    }

    companion object {
        const val CHANNEL_ID = "ActivityTrackingChannel"
        const val NOTIFICATION_ID = 1
    }
}
