package com.example.activity_tracker_dv.home.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.activity_tracker_dv.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.example.activity_tracker_dv.data.AppDatabase
import com.example.activity_tracker_dv.repository.EventRepository

class SettingsFragment : Fragment() {

    private lateinit var syncButton: Button
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("SettingsFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        syncButton = view.findViewById(R.id.button_sync_database)
        logoutButton = view.findViewById(R.id.button_logout)

        Log.d("SettingsFragment", "Buttons initialized")

        // Sincronizza il database Room con Firebase per l'utente autenticato
        syncButton.setOnClickListener {
            Log.d("SettingsFragment", "Sync button clicked")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("SettingsFragment", "Starting database synchronization")
                    // Configura il ViewModel e il Repository
                    val eventDao = AppDatabase.getDatabase(requireContext()).eventDao()
                    Log.d("SettingsFragment", "EventDao initialized")
                    val repository = EventRepository(eventDao)
                    Log.d("SettingsFragment", "EventRepository initialized")
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val userUsername = currentUser?.email ?: ""
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

        // Esegui il logout dell'utente
        logoutButton.setOnClickListener {
            Log.d("SettingsFragment", "Logout button clicked")
            FirebaseAuth.getInstance().signOut()
            Log.d("SettingsFragment", "User signed out successfully")
            activity?.finish()
            Log.d("SettingsFragment", "Activity finished after logout")
            Toast.makeText(requireContext(), "Logout effettuato con successo.", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
