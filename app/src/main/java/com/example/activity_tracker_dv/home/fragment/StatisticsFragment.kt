package com.example.activity_tracker_dv.home.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.activity_tracker_dv.R

class StatisticsFragment : Fragment() {

    private var isColorChanged = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)
        val button = view.findViewById<Button>(R.id.button_change_color)

        button.setOnClickListener {
            if (isColorChanged) {
                view.setBackgroundColor(Color.WHITE)  // Cambia a bianco
            } else {
                view.setBackgroundColor(Color.GREEN) // Cambia a verde
            }
            isColorChanged = !isColorChanged
        }

        return view
    }
}
