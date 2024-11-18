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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class EventRepository(private val eventDao: EventDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("events")

    private val syncStatus = MutableLiveData<Boolean>()

    suspend fun insert(event: Event): Long {
        val id = eventDao.insert(event)
        saveEventToFirestore(event.copy(id = id))
        return id
    }

    suspend fun update(event: Event) {
        eventDao.update(event)
        saveEventToFirestore(event)
    }

    suspend fun delete(event: Event) {
        eventDao.delete(event)
        deleteEventFromFirestore(event.id)
    }

    fun getEventById(id: Long): LiveData<Event> {
        return eventDao.getEventById(id)
    }

    fun getAllEvents(): LiveData<List<Event>> {
        return eventDao.getAllEvents()
    }

    fun getEventsForUser(userUsername: String): LiveData<List<Event>> {
        return eventDao.getEventsByUser(userUsername)
    }

    fun getSyncStatus(): LiveData<Boolean> {
        return syncStatus
    }

    suspend fun synchronizeWithFirebaseForUser(userUsername: String) {
        val firebaseEvents = mutableListOf<Event>()

        try {
            val snapshot = collection.whereEqualTo("userUsername", userUsername).get().await()
            for (document in snapshot.documents) {
                val event = document.toObject(Event::class.java)
                event?.let { firebaseEvents.add(it) }
            }

            withContext(Dispatchers.IO) {
                val localEvents = eventDao.getEventsByUser(userUsername).value ?: listOf()
                val firebaseEventIds = firebaseEvents.map { it.id }

                val eventsToAdd = firebaseEvents.filter { firebaseEvent ->
                    localEvents.none { localEvent: Event -> localEvent.id == firebaseEvent.id }
                }

                val eventsToRemove = localEvents.filter { localEvent ->
                    firebaseEventIds.none { it: Long -> it == localEvent.id }
                }

                eventDao.deleteAll(eventsToRemove)

                eventDao.insertAll(eventsToAdd)

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

    private suspend fun saveEventToFirestore(event: Event) {
        collection.document(event.id.toString()).set(event).await()
    }

    private suspend fun deleteEventFromFirestore(id: Long) {
        collection.document(id.toString()).delete().await()
    }

    fun getEventsForUserFromFirebase(userUsername: String): LiveData<List<Event>> {
        val liveData = MutableLiveData<List<Event>>()

        // Avvio di una coroutine per il recupero dati
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firebaseEvents = mutableListOf<Event>()
                val snapshot = collection.whereEqualTo("userUsername", userUsername).get().await()

                for (document in snapshot.documents) {
                    val event = document.toObject(Event::class.java)
                    event?.let { firebaseEvents.add(it) }
                }

                // Aggiorna LiveData con i risultati
                liveData.postValue(firebaseEvents)
            } catch (e: Exception) {
                Log.e("EventRepository", "Errore nel recupero eventi con Firebase: ${e.message}")
                liveData.postValue(emptyList()) // In caso di errore, emetti una lista vuota
            }
        }

        return liveData
    }


}
