package com.example.activity_tracker_dv.home.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.services.ActivityTrackingService
import com.example.activity_tracker_dv.viewmodels.EventViewModel
import com.example.activity_tracker_dv.viewmodels.EventViewModelFactory
import com.example.activity_tracker_dv.repository.EventRepository
import com.example.activity_tracker_dv.data.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import java.util.Locale

class ActivityFragment : Fragment() {

    private lateinit var activityTextView: TextView
    private lateinit var stepsTextView: TextView  // Nuovo TextView per il conteggio dei passi
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var eventViewModel: EventViewModel
    private lateinit var previousActivitiesListView: ListView
    private lateinit var eventsAdapter: ArrayAdapter<String>
    private var rootView: View? = null

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allPermissionsGranted = true
        permissions.entries.forEach {
            if (!it.value) {
                allPermissionsGranted = false
                Log.d("ActivityFragment", "Permesso ${it.key} negato.")
            }
        }
        if (allPermissionsGranted) {
            Log.d("ActivityFragment", "Tutti i permessi necessari concessi.")
            startTracking()
        } else {
            Log.d("ActivityFragment", "Permessi mancanti per il monitoraggio.")
            activityTextView.text = "Permessi mancanti per il monitoraggio."
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            Log.d("ActivityFragment", "onCreateView chiamato per creare la vista.")
            rootView = inflater.inflate(R.layout.fragment_activity, container, false)

            activityTextView = rootView!!.findViewById(R.id.activity_text)
            stepsTextView = rootView!!.findViewById(R.id.steps_text)  // Inizializzazione del nuovo TextView
            startButton = rootView!!.findViewById(R.id.start_button)
            stopButton = rootView!!.findViewById(R.id.stop_button)
            previousActivitiesListView = rootView!!.findViewById(R.id.previous_activities_list)

            val eventDao = AppDatabase.getDatabase(requireContext()).eventDao()
            val repository = EventRepository(eventDao)
            val factory = EventViewModelFactory(repository)
            eventViewModel = ViewModelProvider(this, factory).get(EventViewModel::class.java)

            eventsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
            previousActivitiesListView.adapter = eventsAdapter

            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUsername = currentUser?.email ?: ""

            // Osserva i cambiamenti degli eventi salvati per l'utente autenticato e aggiorna l'UI
            eventViewModel.getEventsForUser(userUsername).observe(viewLifecycleOwner, Observer { events ->
                Log.d("ActivityFragment", "Osservando cambiamenti degli eventi per l'utente: $userUsername.")
                if (events.isNotEmpty()) {
                    val eventDescriptions = events.map { event ->
                        val formattedDistance = String.format(Locale.getDefault(), "%.3f", event.distanceTravelled)
                        "Attività: ${event.eventType}, Inizio: ${event.launch}, Fine: ${event.end}, Distanza: ${formattedDistance} km, Passi: ${event.steps}"
                    }
                    eventsAdapter.clear()
                    eventsAdapter.addAll(eventDescriptions)
                    eventsAdapter.notifyDataSetChanged()
                } else {
                    activityTextView.text = "Nessuna attività registrata"
                    stepsTextView.text = "Passi: 0"
                    Log.d("ActivityFragment", "Nessuna attività registrata.")
                }
            })

            // Osserva i cambiamenti dell'evento corrente per aggiornare l'UI sopra i pulsanti
            eventViewModel.currentEvent.observe(viewLifecycleOwner, Observer { currentEvent ->
                currentEvent?.let {
                    val formattedDistance = String.format(Locale.getDefault(), "%.3f", currentEvent.distanceTravelled)
                    activityTextView.text = "Attività: ${currentEvent.eventType}, Distanza: ${formattedDistance} km"
                    stepsTextView.text = "Passi: ${currentEvent.steps}"
                    Log.d("ActivityFragment", "Evento corrente: ${currentEvent.eventType}, Distanza: ${formattedDistance} km, Passi: ${currentEvent.steps}")
                } ?: run {
                    activityTextView.text = "Nessuna attività corrente"
                    stepsTextView.text = "Passi: 0"
                }
            })

            startButton.setOnClickListener {
                Log.d("ActivityFragment", "Pulsante Start cliccato.")
                checkPermissionsAndStartTracking()
            }

            stopButton.setOnClickListener {
                Log.d("ActivityFragment", "Pulsante Stop cliccato.")
                stopTracking()
            }
        } else {
            Log.d("ActivityFragment", "onCreateView chiamato ma la vista era già esistente.")
        }
        return rootView
    }

    private fun checkPermissionsAndStartTracking() {
        Log.d("ActivityFragment", "Controllo dei permessi necessari.")
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d("ActivityFragment", "Richiesta dei seguenti permessi: ${permissionsToRequest.joinToString()}.")
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("ActivityFragment", "Tutti i permessi necessari già concessi.")
            startTracking()
        }
    }

    private fun startTracking() {
        Log.d("ActivityFragment", "Avvio del monitoraggio delle attività.")
        val intent = Intent(requireContext(), ActivityTrackingService::class.java)
        requireContext().startService(intent)
        activityTextView.text = "Monitoraggio attività avviato."
        stepsTextView.text = "Passi: 0"  // Reset dei passi all'inizio del tracking
        Log.d("ActivityFragment", "Monitoraggio attività avviato.")
    }

    private fun stopTracking() {
        Log.d("ActivityFragment", "Interruzione del monitoraggio delle attività.")
        val intent = Intent(requireContext(), ActivityTrackingService::class.java)
        requireContext().stopService(intent)
        activityTextView.text = "Monitoraggio attività fermato."
        stepsTextView.text = "Passi: 0"
        Log.d("ActivityFragment", "Monitoraggio attività fermato.")
    }
}
