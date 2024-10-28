package com.example.activity_tracker_dv.repository

import androidx.lifecycle.LiveData
import com.example.activity_tracker_dv.data.EventDao
import com.example.activity_tracker_dv.models.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EventRepository(private val eventDao: EventDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("events")

    // Inserimento dell'evento in Room e Firebase Firestore
    suspend fun insert(event: Event): Long {
        val id = eventDao.insert(event)
        saveEventToFirestore(event.copy(id = id))
        return id
    }

    // Aggiorna un evento
    suspend fun update(event: Event) {
        eventDao.update(event)
        saveEventToFirestore(event)
    }

    // Cancella un evento
    suspend fun delete(event: Event) {
        eventDao.delete(event)
        deleteEventFromFirestore(event.id)
    }

    // Recupera un evento da Room
    fun getEventById(id: Long): LiveData<Event> {
        return eventDao.getEventById(id)
    }

    // Recupera tutti gli eventi da Room
    fun getAllEvents(): LiveData<List<Event>> {
        return eventDao.getAllEvents()
    }

    // Salva un evento su Firebase Firestore
    private suspend fun saveEventToFirestore(event: Event) {
        collection.document(event.id.toString()).set(event).await()
    }

    // Cancella un evento da Firebase Firestore
    private suspend fun deleteEventFromFirestore(id: Long) {
        collection.document(id.toString()).delete().await()
    }
}
