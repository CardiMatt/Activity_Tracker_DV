package com.example.activity_tracker_dv.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.activity.ActivityStateManager
import android.widget.TextView

class ActivityFragment : Fragment() {

    private lateinit var activityTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity, container, false)
        activityTextView = view.findViewById(R.id.activity_text)

        // Osserva i cambiamenti di stato dell'attività
        ActivityStateManager.currentActivity.observe(viewLifecycleOwner, Observer { activity ->
            activityTextView.text = "Attività corrente: $activity"
        })

        return view
    }
}
