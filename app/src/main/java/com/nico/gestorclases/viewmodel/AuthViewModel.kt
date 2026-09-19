package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.nico.gestorclases.data.repository.AuthRepository
import com.nico.gestorclases.data.sync.CloudSyncManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    private val cloudSyncManager: CloudSyncManager
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = repository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        // Arranca/detiene el sync automáticamente según el estado de sesión
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) cloudSyncManager.startSyncing(user)
                else cloudSyncManager.stopSyncing()
            }
        }
    }

    /**
     * Inicia el flujo de autenticación con Google.
     *
     * El [android.content.Context] se pasa desde el Composable (donde vive) y NO se
     * almacena en el ViewModel. Esto respeta la regla de no tener referencias al ciclo
     * de vida de la Activity desde el ViewModel.
     *
     * [onResult] se invoca en el hilo principal con `true` si el login fue exitoso
     * (incluye el caso donde el usuario cerró el popup voluntariamente → `false`).
     */
    fun signInWithGoogle(
        context: android.content.Context,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.signInWithGoogle(context)
            onResult(result.getOrDefault(false))
        }
    }

    fun signOut() {
        repository.signOut()
    }
}
