package com.example.activity_tracker_dv.home.fragment

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TimePicker
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.services.DailyNotificationService
import com.example.activity_tracker_dv.viewmodels.EventViewModel
import com.example.activity_tracker_dv.viewmodels.EventViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.example.activity_tracker_dv.data.AppDatabase
import com.example.activity_tracker_dv.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var syncButton: Button
    private lateinit var logoutButton: Button
    private lateinit var setNotificationButton: Button
    private lateinit var exportButton: Button
    private lateinit var eventViewModel: EventViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("SettingsFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize buttons
        syncButton = view.findViewById(R.id.button_sync_database)
        logoutButton = view.findViewById(R.id.button_logout)
        setNotificationButton = view.findViewById(R.id.button_set_notification)
        exportButton = view.findViewById(R.id.button_export_data)

        Log.d("SettingsFragment", "Buttons initialized")

        // Configure ViewModel
        val eventDao = AppDatabase.getDatabase(requireContext()).eventDao()
        val repository = EventRepository(eventDao)
        val factory = EventViewModelFactory(repository)
        eventViewModel = ViewModelProvider(this, factory).get(EventViewModel::class.java)

        // Sync Room database with Firebase for authenticated user
        syncButton.setOnClickListener {
            Log.d("SettingsFragment", "Sync button clicked")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("SettingsFragment", "Starting database synchronization")
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val userUsername = currentUser?.email ?: ""
                    Log.d("SettingsFragment", "Current user: $userUsername")
                    repository.synchronizeWithFirebaseForUser(userUsername)
                    Log.d("SettingsFragment", "Synchronization process initiated for user: $userUsername")
                    withContext(Dispatchers.Main) {
                        Log.d("SettingsFragment", "Synchronization completed successfully")
                        Toast.makeText(requireContext(), "Sincronizzazione completata con successo.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("SettingsFragment", "Errore durante la sincronizzazione del database: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Errore durante la sincronizzazione del database.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Log out the user
        logoutButton.setOnClickListener {
            Log.d("SettingsFragment", "Logout button clicked")
            FirebaseAuth.getInstance().signOut() // Sign out the current user
            Log.d("SettingsFragment", "User signed out successfully")
            activity?.finish() // Finish the activity to prevent going back to authenticated views
            Log.d("SettingsFragment", "Activity finished after logout")
            Toast.makeText(requireContext(), "Logout effettuato con successo.", Toast.LENGTH_SHORT).show()
        }

        // Set up daily notification
        setNotificationButton.setOnClickListener {
            Log.d("SettingsFragment", "Set Notification button clicked")
            val timePickerDialog = TimePickerDialog(requireContext(), { _: TimePicker, hour: Int, minute: Int ->
                Log.d("SettingsFragment", "Orario scelto per la notifica: $hour:$minute")
                // Create an intent to start the DailyNotificationService
                val intent = Intent(requireContext(), DailyNotificationService::class.java).apply {
                    putExtra(DailyNotificationService.EXTRA_HOUR, hour)
                    putExtra(DailyNotificationService.EXTRA_MINUTE, minute)
                }
                // Start the service depending on Android version
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    Log.d("SettingsFragment", "Starting foreground service for notification")
                    requireContext().startForegroundService(intent)
                } else {
                    Log.d("SettingsFragment", "Starting service for notification")
                    requireContext().startService(intent)
                }
                Toast.makeText(requireContext(), "Notifica giornaliera impostata alle $hour:$minute", Toast.LENGTH_SHORT).show()
            }, 12, 0, true) // Default time is 12:00
            timePickerDialog.show()
            Log.d("SettingsFragment", "TimePickerDialog shown")
        }

        // Export activity data to a file
        exportButton.setOnClickListener {
            Log.d("SettingsFragment", "Export button clicked")
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUsername = currentUser?.email ?: ""
            eventViewModel.getEventsForUser(userUsername).observe(viewLifecycleOwner) { events ->
                lifecycleScope.launch {
                    try {
                        Log.d("SettingsFragment", "Starting export of activity data")
                        Log.d("SettingsFragment", "Fetched ${events.size} events for user: $userUsername")
                        // Create a file in the Documents directory to save exported data
                        val documentsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val exportFile = File(documentsDir, "exported_activities_$timestamp.txt")

                        FileOutputStream(exportFile).use { output ->
                            // Write each event's data to the file
                            events.forEach { event ->
                                val eventString = "Attività: ${event.eventType}, Inizio: ${event.launch}, Fine: ${event.end}, Distanza: ${event.distanceTravelled} km\n"
                                output.write(eventString.toByteArray())
                            }
                        }
                        Log.d("SettingsFragment", "Data written to file: ${exportFile.absolutePath}")
                        withContext(Dispatchers.Main) {
                            // Notify the user and make the file accessible through the FileProvider
                            val uri = FileProvider.getUriForFile(requireContext(), "com.example.activity_tracker_dv.fileprovider", exportFile)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(shareIntent, "Condividi il file esportato"))
                            Toast.makeText(requireContext(), "Dati esportati in ${exportFile.absolutePath}", Toast.LENGTH_SHORT).show()
                            Log.d("SettingsFragment", "Dati esportati con successo in ${exportFile.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsFragment", "Errore durante l'esportazione dei dati: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Errore durante l'esportazione dei dati.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        return view
    }
}
