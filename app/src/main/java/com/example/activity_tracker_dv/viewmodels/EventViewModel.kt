package com.example.activity_tracker_dv.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activity_tracker_dv.models.Event
import com.example.activity_tracker_dv.repository.EventRepository
import kotlinx.coroutines.launch

class EventViewModel(private val repository: EventRepository) : ViewModel() {

    suspend fun insertEvent(event: Event): Long {
        return repository.insert(event)
    }

    suspend fun updateEvent(event: Event) = viewModelScope.launch {
        repository.update(event)
    }

    suspend fun deleteEvent(event: Event) = viewModelScope.launch {
        repository.delete(event)
    }

    fun getEventById(id: Long): LiveData<Event> {
        return repository.getEventById(id)
    }

    fun getAllEvents(): LiveData<List<Event>> {
        return repository.getAllEvents()
    }
}
