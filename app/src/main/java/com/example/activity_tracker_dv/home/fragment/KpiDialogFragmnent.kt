package com.example.activity_tracker_dv.home.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.viewmodels.EventViewModel
import android.view.LayoutInflater
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.activity_tracker_dv.data.AppDatabase
import com.example.activity_tracker_dv.repository.EventRepository
import com.example.activity_tracker_dv.viewmodels.EventViewModelFactory

class KpiDialogFragment(private val followedEmail: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_kpi, null)

            val eventDao = AppDatabase.getDatabase(requireContext()).eventDao()
            val eventRepository = EventRepository(eventDao)
            val eventFactory = EventViewModelFactory(eventRepository)
            var eventViewModel = ViewModelProvider(this, eventFactory)[EventViewModel::class.java]

            val totalActivitiesTextView = view.findViewById<TextView>(R.id.text_view_total_activities)
            val totalDistanceTextView = view.findViewById<TextView>(R.id.text_view_total_distance)
            val totalStepsTextView = view.findViewById<TextView>(R.id.text_view_total_steps)

            eventViewModel.getEventsForUserFromFirebase(followedEmail).observe(this, Observer { events ->
                val totalActivities = events.size
                val totalDistance = events.sumOf { it.distanceTravelled }
                val totalSteps = events.sumOf { it.steps }

                totalActivitiesTextView.text = "Totale Attività: $totalActivities"
                totalDistanceTextView.text = "Distanza Totale: $totalDistance km"
                totalStepsTextView.text = "Passi Totali: $totalSteps"
            })

            builder.setView(view)
                .setPositiveButton("Chiudi") { dialog, _ ->
                    dialog.dismiss()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
