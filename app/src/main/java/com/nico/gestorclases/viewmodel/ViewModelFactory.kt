package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.ClaseRepository

class ViewModelFactory(
    private val alumnoRepository: AlumnoRepository,
    private val claseRepository: ClaseRepository
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

            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
    }
}
