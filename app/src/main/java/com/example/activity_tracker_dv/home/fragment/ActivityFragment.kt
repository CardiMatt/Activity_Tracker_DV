package com.example.activity_tracker_dv.home.fragment

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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.services.EventRecognitionReceiver
import com.example.activity_tracker_dv.viewmodels.EventViewModel
import com.example.activity_tracker_dv.viewmodels.EventViewModelFactory
import com.example.activity_tracker_dv.repository.EventRepository
import com.example.activity_tracker_dv.data.AppDatabase
import com.google.firebase.auth.FirebaseAuth

class ActivityFragment : Fragment() {

    private lateinit var activityTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var eventViewModel: EventViewModel
    private lateinit var previousActivitiesListView: ListView
    private lateinit var eventsAdapter: ArrayAdapter<String>
    private var rootView: View? = null

    // Inizializza il launcher per la richiesta dei permessi
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("ActivityFragment", "Permessi per la localizzazione concessi.")
            startTracking()
        } else {
            Log.d("ActivityFragment", "Permessi per la localizzazione negati.")
            activityTextView.text = "Permessi per la localizzazione negati."
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
            startButton = rootView!!.findViewById(R.id.start_button)
            stopButton = rootView!!.findViewById(R.id.stop_button)
            previousActivitiesListView = rootView!!.findViewById(R.id.previous_activities_list)

            // Configura il ViewModel e il Repository
            val eventDao = AppDatabase.getDatabase(requireContext()).eventDao()
            val repository = EventRepository(eventDao)
            val factory = EventViewModelFactory(repository)
            eventViewModel = ViewModelProvider(this, factory).get(EventViewModel::class.java)

            // Configura l'adapter per la ListView degli eventi precedenti
            eventsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
            previousActivitiesListView.adapter = eventsAdapter

            // Recupera l'utente autenticato
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUsername = currentUser?.email ?: ""

            // Osserva i cambiamenti degli eventi salvati per l'utente autenticato e aggiorna l'UI
            eventViewModel.getEventsForUser(userUsername).observe(viewLifecycleOwner, Observer { events ->
                Log.d("ActivityFragment", "Osservando cambiamenti degli eventi per l'utente: $userUsername.")
                // Aggiorna l'interfaccia utente con i dati dell'ultimo evento
                if (events.isNotEmpty()) {
                    val lastEvent = events.last()
                    activityTextView.text = "Attività: ${lastEvent.eventType}, Distanza: ${lastEvent.distanceTravelled} km"
                    Log.d("ActivityFragment", "Ultimo evento osservato: ${lastEvent.eventType}, Distanza: ${lastEvent.distanceTravelled} km")

                    // Aggiorna la lista degli eventi precedenti
                    val eventDescriptions = events.map { event ->
                        "Attività: ${event.eventType}, Inizio: ${event.launch}, Fine: ${event.end}, Distanza: ${event.distanceTravelled} km"
                    }
                    eventsAdapter.clear()
                    eventsAdapter.addAll(eventDescriptions)
                    eventsAdapter.notifyDataSetChanged()
                } else {
                    activityTextView.text = "Nessuna attività registrata"
                    Log.d("ActivityFragment", "Nessuna attività registrata.")
                }
            })

            // Avvia il servizio quando il pulsante di inizio è cliccato
            startButton.setOnClickListener {
                Log.d("ActivityFragment", "Pulsante Start cliccato.")
                checkLocationPermissionAndStartTracking()
            }

            // Interrompi il servizio quando il pulsante di stop è cliccato
            stopButton.setOnClickListener {
                Log.d("ActivityFragment", "Pulsante Stop cliccato.")
                stopTracking()
            }
        } else {
            Log.d("ActivityFragment", "onCreateView chiamato ma la vista era già esistente.")
        }
        return rootView
    }

    // Controlla i permessi di localizzazione e avvia il tracking
    private fun checkLocationPermissionAndStartTracking() {
        Log.d("ActivityFragment", "Controllo dei permessi di localizzazione.")
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("ActivityFragment", "Permessi di localizzazione già concessi.")
            startTracking()
        } else {
            Log.d("ActivityFragment", "Richiesta dei permessi di localizzazione.")
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Avvia il tracking utilizzando EventRecognitionReceiver
    private fun startTracking() {
        Log.d("ActivityFragment", "Avvio del monitoraggio delle attività.")
        EventRecognitionReceiver.startActivityTransitionUpdates(requireContext())
        activityTextView.text = "Monitoraggio attività avviato."
        Log.d("ActivityFragment", "Monitoraggio attività avviato.")
    }

    // Ferma il tracking utilizzando EventRecognitionReceiver
    private fun stopTracking() {
        Log.d("ActivityFragment", "Interruzione del monitoraggio delle attività.")
        EventRecognitionReceiver.stopActivityTransitionUpdates(requireContext())
        activityTextView.text = "Monitoraggio attività fermato."
        Log.d("ActivityFragment", "Monitoraggio attività fermato.")
    }
}
