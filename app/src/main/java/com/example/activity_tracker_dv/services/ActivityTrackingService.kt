package com.example.activity_tracker_dv.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.home.HomeActivity
import com.example.activity_tracker_dv.home.fragment.ActivityFragment
import com.example.activity_tracker_dv.viewmodels.EventViewModel

class ActivityTrackingService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("ActivityTrackingService", "Service creato")
        createNotificationChannel() // Creiamo il canale di notifica
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ActivityTrackingService", "Avvio del monitoraggio delle attività e della posizione")
        eventViewModel?.let {
            EventRecognitionReceiver.startActivityTransitionUpdates(applicationContext,
                it
            )
        }

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoraggio attività in corso")
            .setContentText("L'app sta monitorando le tue attività fisiche.")
            .setSmallIcon(R.drawable.stats_18)  // Assicurati che l'icona esista
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Channel di Monitoraggio Attività",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
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
        const val NOTIFICATION_ID = 2
        private var eventViewModel: EventViewModel? = null

        fun setViewModel(viewModel: EventViewModel) {
            eventViewModel = viewModel
        }
    }
}
