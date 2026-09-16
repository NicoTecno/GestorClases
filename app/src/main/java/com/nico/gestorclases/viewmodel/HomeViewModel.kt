package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.ClaseConAlumnos
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.ClaseRepository
import com.nico.gestorclases.utils.DateUtils.toStartOfDayMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val alumnoRepository: AlumnoRepository,
    private val claseRepository: ClaseRepository
) : ViewModel() {

    private val hoy: LocalDate = LocalDate.now()
    private val hoyMillis: Long = hoy.toStartOfDayMillis()

    val clasesDeHoy: StateFlow<List<ClaseConAlumnos>> =
        claseRepository.getClasesDelDia(hoyMillis)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todosLosAlumnos: StateFlow<List<Alumno>> =
        alumnoRepository.todosLosAlumnos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun agregarClase(clase: Clase, crossRefs: List<ClaseAlumnoCrossRef>) = viewModelScope.launch {
        claseRepository.insertarClaseConAlumnos(clase, crossRefs)
    }

    fun actualizarClase(clase: Clase) = viewModelScope.launch {
        claseRepository.actualizarClase(clase)
    }

    fun actualizarClaseConAlumnos(clase: Clase, crossRefs: List<ClaseAlumnoCrossRef>) =
        viewModelScope.launch {
            claseRepository.actualizarClaseConAlumnos(clase, crossRefs)
        }

    fun marcarPagado(crossRef: ClaseAlumnoCrossRef) = viewModelScope.launch {
        claseRepository.actualizarParticipante(
            crossRef.copy(estadoPago = com.nico.gestorclases.data.model.EstadoPago.PAGADA)
        )
    }

    fun eliminarClase(clase: Clase) = viewModelScope.launch {
        claseRepository.eliminarClase(clase)
    }

    suspend fun validarSolapamiento(
        fecha: Long,
        horaInicio: String,
        horaFin: String,
        claseIdIgnorar: Int = 0
    ): Boolean = claseRepository.haySolapamientoDeHorario(fecha, horaInicio, horaFin, claseIdIgnorar)
}
