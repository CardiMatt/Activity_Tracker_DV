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

    // Aggiungi currentEvent per mantenere lo stato dell'evento in corso
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

    // Funzione per impostare il currentEvent
    fun setCurrentEvent(event: Event?) {
        Log.d("EvemtViewModel", "setCurrentEvent chiamato con: $event")
        _currentEvent.postValue(event)
    }

    // Nuovo metodo per ottenere gli eventi per l'utente autenticato
    fun getEventsForUser(userUsername: String): LiveData<List<Event>> {
        return repository.getEventsForUser(userUsername)
    }
}
