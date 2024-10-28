package com.example.activity_tracker_dv.repository

import androidx.lifecycle.LiveData
import android.util.Log
import com.example.activity_tracker_dv.data.EventDao
import com.example.activity_tracker_dv.models.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.lifecycle.MutableLiveData

class EventRepository(private val eventDao: EventDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("events")

    private val syncStatus = MutableLiveData<Boolean>()

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

    // Recupera tutti gli eventi per un determinato utente
    fun getEventsForUser(userUsername: String): LiveData<List<Event>> {
        return eventDao.getEventsByUser(userUsername)
    }

    // Restituisce lo stato della sincronizzazione
    fun getSyncStatus(): LiveData<Boolean> {
        return syncStatus
    }

    // Sincronizza i dati tra Firebase e Room per un determinato utente
    suspend fun synchronizeWithFirebaseForUser(userUsername: String) {
        val firebaseEvents = mutableListOf<Event>()

        try {
            val snapshot = collection.whereEqualTo("userUsername", userUsername).get().await()
            for (document in snapshot.documents) {
                val event = document.toObject(Event::class.java)
                event?.let { firebaseEvents.add(it) }
            }

            withContext(Dispatchers.IO) {
                val localEvents = eventDao.getEventsByUser(userUsername).value ?: listOf() // Ottieni una lista non LiveData
                val firebaseEventIds = firebaseEvents.map { it.id }

                // Eventi da aggiungere a Room che sono presenti solo su Firebase
                val eventsToAdd = firebaseEvents.filter { firebaseEvent ->
                    localEvents.none { localEvent: Event -> localEvent.id == firebaseEvent.id }
                }

                // Eventi da rimuovere da Room che non sono più presenti su Firebase
                val eventsToRemove = localEvents.filter { localEvent ->
                    firebaseEventIds.none { it: Long -> it == localEvent.id }
                }

                // Rimuovi gli eventi da Room
                eventDao.deleteAll(eventsToRemove)

                // Aggiungi gli eventi mancanti a Room
                eventDao.insertAll(eventsToAdd)

                // Aggiorna lo stato della sincronizzazione
                withContext(Dispatchers.Main) {
                    syncStatus.value = true
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("EventRepository", "Errore nella sincronizzazione con Firebase: ${e.message}")
                syncStatus.value = false
            }
        }
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
