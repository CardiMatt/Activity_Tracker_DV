/*
package com.example.activity_tracker_dv.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.activity_tracker_dv.data.AppDatabase
import com.example.activity_tracker_dv.models.Event
import com.example.activity_tracker_dv.repository.EventRepository
import com.example.activity_tracker_dv.viewmodels.EventViewModel
import com.example.activity_tracker_dv.viewmodels.EventViewModelFactory
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class ActivityRecognitionService : Service(), SensorEventListener {

    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private lateinit var eventRepository: EventRepository
    private lateinit var eventViewModel: EventViewModel
    private val viewModelStore = ViewModelStore()

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private var initialSteps: Int = -1
    private var currentSteps: Int = 0

    private var eventId: Long? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var startLocation: Location? = null
    private var endLocation: Location? = null

    private var userEmail: String? = null

    override fun onCreate() {
        super.onCreate()

        // Inizializza manualmente EventRepository e ViewModel
        val eventDao = AppDatabase.getDatabase(this).eventDao()
        eventRepository = EventRepository(eventDao)
        val factory = EventViewModelFactory(eventRepository)
        eventViewModel = ViewModelProvider(viewModelStore, factory)[EventViewModel::class.java]

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Log.d("ActivityRecognition", "Step counter sensor not available")
        }

        // Initialize Firebase Auth and get the user email
        userEmail = FirebaseAuth.getInstance().currentUser?.email

        if (userEmail == null) {
            Log.d("ActivityRecognition", "User email is not available")
            stopSelf()
            return
        }

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        activityRecognitionClient = ActivityRecognition.getClient(this)

        val selectedActivityType = intent?.getStringExtra("selectedActivity") ?: "unknown"
        if (selectedActivityType == "walk" || selectedActivityType == "run") {
            startStepTracking()

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    startLocation = it
                    Log.d("ActivityRecognition", "Start location: ${startLocation?.latitude}, ${startLocation?.longitude}")
                }
            }
        }

        val event = Event(
            userUsername = userEmail!!, // Use Firebase user email as identifier
            eventType = selectedActivityType,
            steps = 0,
            launch = Date(),
            end = Date(),
            startLatitude = startLocation?.latitude ?: 0.0,
            startLongitude = startLocation?.longitude ?: 0.0,
            endLatitude = 0.0,  // Will be updated on stop
            endLongitude = 0.0  // Will be updated on stop
        )
        CoroutineScope(Dispatchers.IO).launch {
            eventId = eventViewModel.insertEvent(event)
        }
        return START_STICKY
    }

    private fun startStepTracking() {
        Log.d("ActivityRecognition", "Starting step tracking")
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stopStepTracking() {
        Log.d("ActivityRecognition", "Stopping step tracking")
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val stepsSinceReboot = event.values[0].toInt()
            if (initialSteps == -1) {
                initialSteps = stepsSinceReboot
            }
            currentSteps = stepsSinceReboot - initialSteps
            Log.d("ActivityRecognition", "Current steps: $currentSteps")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        stopStepTracking()

        // Capture end location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                endLocation = it
                Log.d("ActivityRecognition", "End location: ${endLocation?.latitude}, ${endLocation?.longitude}")

                // Update the event with the total steps and location
                eventId?.let { id ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val event = eventViewModel.getEventById(id).value
                        event?.let {
                            it.steps = currentSteps
                            it.end = Date() // Update the end time of the event
                            it.endLatitude = endLocation?.latitude ?: 0.0
                            it.endLongitude = endLocation?.longitude ?: 0.0
                            eventViewModel.updateEvent(it)
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
*/