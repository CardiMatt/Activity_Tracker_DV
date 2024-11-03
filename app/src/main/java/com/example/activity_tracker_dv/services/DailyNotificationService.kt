package com.example.activity_tracker_dv.services

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.activity_tracker_dv.R
import java.util.Calendar

class DailyNotificationService : Service() {

    companion object {
        const val REMINDER_CHANNEL_ID = "DailyReminderChannel"
        const val EXTRA_HOUR = "EXTRA_HOUR"
        const val EXTRA_MINUTE = "EXTRA_MINUTE"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("DailyNotificationService", "Service creato per promemoria giornaliero")
        createNotificationChannel()

        // Start foreground service with notification
        val notification = createNotification("Promemoria giornaliero in esecuzione")
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("DailyNotificationService", "Servizio di promemoria giornaliero avviato")
        intent?.let {
            val hour = it.getIntExtra(EXTRA_HOUR, 12)
            val minute = it.getIntExtra(EXTRA_MINUTE, 0)
            scheduleDailyReminderNotification(hour, minute)
        }
        return START_STICKY
    }

    private fun scheduleDailyReminderNotification(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Log.d("DailyNotificationService", "Promemoria giornaliero schedulato alle $hour:$minute")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DailyNotificationService", "Servizio di promemoria giornaliero distrutto")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Daily Reminder Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(reminderChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
            .setContentTitle("Activity Tracker Promemoria")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.stats_18)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}

// Receiver per gestire l'invio della notifica giornaliera
class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, DailyNotificationService.REMINDER_CHANNEL_ID)
            .setContentTitle("Promemoria Attività")
            .setContentText("Inizia a registrare le tue attività!")
            .setSmallIcon(R.drawable.stats_18)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(DailyNotificationService.NOTIFICATION_ID, notification)
        Log.d("ReminderNotificationReceiver", "Notifica giornaliera inviata")
    }
}
