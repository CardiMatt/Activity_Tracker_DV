package com.example.activity_tracker_dv.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activity_tracker_dv.models.Event
import com.example.activity_tracker_dv.repository.EventRepository
import kotlinx.coroutines.launch

class EventViewModel(private val repository: EventRepository) : ViewModel() {

    private val _currentEvent = MutableLiveData<Event?>()
    val currentEvent: LiveData<Event?> = _currentEvent

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

    fun setCurrentEvent(event: Event?) {
        Log.d("EvemtViewModel", "setCurrentEvent chiamato con: $event")
        _currentEvent.postValue(event)
    }

    fun getEventsForUser(userUsername: String): LiveData<List<Event>> {
        return repository.getEventsForUser(userUsername)
    }

    fun getEventsForUserFromFirebase(userUsername: String): LiveData<List<Event>> {
        return repository.getEventsForUserFromFirebase(userUsername)
    }

}
