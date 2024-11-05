package com.example.activity_tracker_dv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.models.Event

class EventAdapter(private val events: List<Event>) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val eventTypeTextView: TextView = view.findViewById(R.id.eventTypeTextView)
        private val stepsTextView: TextView = view.findViewById(R.id.stepsTextView)

        fun bind(event: Event) {
            eventTypeTextView.text = event.eventType
            stepsTextView.text = "Passi: ${event.steps}"  // Mostra il numero di passi
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_item, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size
}
