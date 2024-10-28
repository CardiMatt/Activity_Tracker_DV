package com.example.activity_tracker_dv.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.activity_tracker_dv.data.AppDatabase
import com.google.android.gms.location.*
import com.example.activity_tracker_dv.models.Event
import com.example.activity_tracker_dv.repository.EventRepository
import com.example.activity_tracker_dv.viewmodels.EventViewModel
import com.example.activity_tracker_dv.viewmodels.EventViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import java.util.Date

class EventRecognitionReceiver : BroadcastReceiver() {

    private var userEmail: String? = null

    companion object {
        private const val TAG = "EventTransitionReceiver"

        private lateinit var activityRecognitionClient: ActivityRecognitionClient
        private lateinit var eventRepository: EventRepository
        private lateinit var eventViewModel: EventViewModel
        private val viewModelStore = ViewModelStore()

        lateinit var fusedLocationClient: FusedLocationProviderClient
        var lastLocation: Location? = null
        var totalDistance = 0.0
        var startTimeMillis: Long = 0

        fun startActivityTransitionUpdates(context: Context) {
            Log.d(TAG, "startActivityTransitionUpdates called")

            val eventDao = AppDatabase.getDatabase(context).eventDao()
            eventRepository = EventRepository(eventDao)
            val factory = EventViewModelFactory(eventRepository)
            eventViewModel = ViewModelProvider(viewModelStore, factory)[EventViewModel::class.java]

            activityRecognitionClient = ActivityRecognition.getClient(context)

            val transitions = listOf(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )

            val request = ActivityTransitionRequest(transitions)
            val intent = Intent(context, EventRecognitionReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            )

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permessi mancanti.")
                return
            }

            Log.d(TAG, "Richiesta di avvio del monitoraggio delle attività")
            activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Richiesta aggiornamenti attività avviata con successo.")
                    resetTrackingData()
                    startLocationUpdates(context)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Errore nella richiesta degli aggiornamenti attività: ${e.message}")
                }
        }

        fun stopActivityTransitionUpdates(context: Context) {
            Log.d(TAG, "stopActivityTransitionUpdates called")
            activityRecognitionClient = ActivityRecognition.getClient(context)
            val intent = Intent(context, EventRecognitionReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            )

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Permessi mancanti durante la rimozione degli aggiornamenti attività.")
                return
            }
            activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Rimozione aggiornamenti attività avviata con successo.")
                    stopLocationUpdates()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Errore nella rimozione degli aggiornamenti attività: ${e.message}")
                }

        }

        fun resetTrackingData() {
            Log.d(TAG, "resetTrackingData called. Dati resettati.")
            totalDistance = 0.0
            lastLocation = null
        }

        @SuppressLint("MissingPermission")
        fun startLocationUpdates(context: Context) {
            Log.d(TAG, "startLocationUpdates called. Avvio del monitoraggio della posizione")
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permessi mancanti per l'accesso alla posizione.")
                return
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }

        fun stopLocationUpdates() {
            Log.d(TAG, "stopLocationUpdates called. Monitoraggio della posizione fermato")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        private val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(TAG, "locationCallback - onLocationResult called with ${locationResult.locations.size} locations")
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.locations.last()
                    Log.d(TAG, "Nuova posizione ricevuta: lat=${location.latitude}, lon=${location.longitude}")
                    lastLocation?.let {
                        val distance = it.distanceTo(location) / 1000 // in km
                        totalDistance += distance
                        Log.d(TAG, "Distanza attuale percorsa: $totalDistance km")
                    }
                    lastLocation = location
                } else {
                    Log.w(TAG, "Nessuna posizione trovata nell'aggiornamento della posizione.")
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) = runBlocking {
        Log.d(TAG, "onReceive called. Intent ricevuto con azione: ${intent.action}")
        if (userEmail == null) {
            userEmail = FirebaseAuth.getInstance().currentUser?.email
            if (userEmail == null) {
                Log.e(TAG, "Impossibile recuperare l'email utente.")
                return@runBlocking
            }
            Log.d(TAG, "Email utente recuperata: $userEmail")
        }
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            Log.d(TAG, "Transizione di attività trovata: $result")
            result?.transitionEvents?.forEach { event ->
                val activityName = when (event.activityType) {
                    DetectedActivity.WALKING -> "Camminata"
                    DetectedActivity.RUNNING -> "Corsa"
                    else -> "Sconosciuto"
                }

                val transitionName = when (event.transitionType) {
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "Inizio"
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "Fine"
                    else -> "Sconosciuto"
                }

                Log.d(TAG, "Transizione rilevata: $activityName ($transitionName)")

                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    Log.d(TAG, "Inizio attività di $activityName")
                    startTimeMillis = System.currentTimeMillis()
                    Log.d(TAG, "Tempo di inizio registrato: $startTimeMillis")

                    val newEvent = Event(
                        userUsername = userEmail ?: "Unknown",
                        eventType = activityName,
                        steps = 0,
                        launch = Date(),
                        end = Date(),
                        startLatitude = lastLocation?.latitude ?: 0.0,
                        startLongitude = lastLocation?.longitude ?: 0.0,
                        endLatitude = 0.0,
                        endLongitude = 0.0
                    )
                    eventViewModel.setCurrentEvent(newEvent)
                    Log.d(TAG, "Evento inizializzato: $newEvent")

                } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                    Log.d(TAG, "Fine attività di $activityName")
                    val endTimeMillis = System.currentTimeMillis()
                    Log.d(TAG, "Tempo di fine registrato: $endTimeMillis")
                    stopLocationUpdates()

                    eventViewModel.currentEvent.value?.let {
                        it.end = Date()
                        it.endLatitude = lastLocation?.latitude ?: 0.0
                        it.endLongitude = lastLocation?.longitude ?: 0.0
                        Log.d(TAG, "Evento aggiornato: $it")

                        val eventId = eventViewModel.insertEvent(it)
                        Log.d(TAG, "Evento Salvato: $eventId")
                        eventViewModel.setCurrentEvent(null)  // Reset currentEvent after saving
                    } ?: run {
                        Log.e(TAG, "Tentativo di concludere un'attività non avviata. Evento nullo.")
                    }
                }
            }
        } else {
            Log.e(TAG, "ActivityTransitionResult non trovato")
        }
        Log.d(TAG, "onReceive - fine elaborazione intent.")
    }
}