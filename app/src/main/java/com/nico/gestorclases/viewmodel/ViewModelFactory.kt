package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.ClaseRepository

import com.nico.gestorclases.data.repository.AuthRepository
import com.nico.gestorclases.data.sync.CloudSyncManager

class ViewModelFactory(
    private val alumnoRepository: AlumnoRepository,
    private val claseRepository: ClaseRepository,
    private val authRepository: AuthRepository,
    private val cloudSyncManager: CloudSyncManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(alumnoRepository, claseRepository) as T

            modelClass.isAssignableFrom(CalendarViewModel::class.java) ->
                CalendarViewModel(alumnoRepository, claseRepository) as T

            modelClass.isAssignableFrom(StudentsViewModel::class.java) ->
                StudentsViewModel(alumnoRepository, claseRepository) as T

            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(authRepository, cloudSyncManager) as T

            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
    }
}
