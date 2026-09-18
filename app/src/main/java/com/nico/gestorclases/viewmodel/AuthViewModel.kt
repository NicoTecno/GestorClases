package com.nico.gestorclases.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.nico.gestorclases.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.nico.gestorclases.data.sync.CloudSyncManager

class AuthViewModel(
    private val repository: AuthRepository,
    private val cloudSyncManager: CloudSyncManager
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = repository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    cloudSyncManager.startSyncing(user)
                } else {
                    cloudSyncManager.stopSyncing()
                }
            }
        }
    }

    fun signInWithGoogle(context: Context, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.signInWithGoogle(context)
            onResult(result.getOrDefault(false))
        }
    }

    fun signOut() {
        repository.signOut()
    }
}
