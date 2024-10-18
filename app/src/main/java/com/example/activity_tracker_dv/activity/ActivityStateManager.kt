package com.example.activity_tracker_dv.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object ActivityStateManager {

    private val _currentActivity = MutableLiveData<String>()
    private val _currentSteps = MutableLiveData<Int>()
    private val _activityStartTime = MutableLiveData<Long>()

    // LiveData per monitorare le attività
    val currentActivity: LiveData<String> = _currentActivity
    val currentSteps: LiveData<Int> = _currentSteps
    val activityStartTime: LiveData<Long> = _activityStartTime

    // Funzioni per aggiornare lo stato dell'attività
    fun updateActivity(activity: String, steps: Int, startTime: Long) {
        _currentActivity.postValue(activity)
        _currentSteps.postValue(steps)
        _activityStartTime.postValue(startTime)
    }

    fun clearActivity() {
        _currentActivity.postValue("")
        _currentSteps.postValue(0)
        _activityStartTime.postValue(0)
    }
}
