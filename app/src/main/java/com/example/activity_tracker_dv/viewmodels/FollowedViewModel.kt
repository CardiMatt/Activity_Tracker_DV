package com.example.activity_tracker_dv.viewmodels

import androidx.lifecycle.*
import com.example.activity_tracker_dv.models.Followed
import com.example.activity_tracker_dv.repository.FollowedRepository
import kotlinx.coroutines.launch

class FollowedViewModel(private val repository: FollowedRepository) : ViewModel() {

    val followedUsers: LiveData<List<Followed>> = repository.getFollowedUsers()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun followUser(followedEmail: String) {
        viewModelScope.launch {
            val result = repository.followUser(followedEmail)
            if (result.isFailure) {
                _error.postValue(result.exceptionOrNull()?.message ?: "Errore durante il follow")
            }
        }
    }

    fun unfollowUser(followedEmail: String) {
        viewModelScope.launch {
            val result = repository.unfollowUser(followedEmail)
            if (result.isFailure) {
                _error.postValue(result.exceptionOrNull()?.message ?: "Errore durante l'unfollow")
            }
        }
    }

    fun synchronizeFollowed() {
        viewModelScope.launch {
            repository.synchronizeFollowed()
        }
    }
}

class FollowedViewModelFactory(private val repository: FollowedRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FollowedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FollowedViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
