package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.ClaseConAlumnos
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.ClaseRepository
import com.nico.gestorclases.utils.DateUtils.toStartOfDayMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val alumnoRepository: AlumnoRepository,
    claseRepository: ClaseRepository
) : ViewModel() {

    /** Delegado con toda la lógica de escritura sobre clases. */
    private val claseActions = ClaseActionsDelegate(claseRepository, viewModelScope)

    /**
     * Fecha "de hoy" como StateFlow mutable.
     * La Screen llama a [refreshFechaHoy] en cada RESUMED para evitar que la fecha
     * se congele si la app pasa la medianoche en background.
     */
    private val _fechaHoy = MutableStateFlow(LocalDate.now())
    val fechaHoy: StateFlow<LocalDate> = _fechaHoy.asStateFlow()

    /** Clases del día reactivas: se re-consultan al cambiar [_fechaHoy]. */
    val clasesDeHoy: StateFlow<List<ClaseConAlumnos>> = _fechaHoy
        .flatMapLatest { fecha ->
            claseRepository.getClasesDelDia(fecha.toStartOfDayMillis())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todosLosAlumnos: StateFlow<List<com.nico.gestorclases.data.model.Alumno>> =
        alumnoRepository.todosLosAlumnos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Actualiza la fecha solo si cambió (evita recomposiciones y queries innecesarias). */
    fun refreshFechaHoy() {
        val today = LocalDate.now()
        if (_fechaHoy.value != today) _fechaHoy.value = today
    }

    // ── Delegación explícita de acciones ──────────────────────────────────────

    fun agregarClase(clase: Clase, crossRefs: List<ClaseAlumnoCrossRef>) =
        claseActions.agregarClase(clase, crossRefs)

    fun actualizarClase(clase: Clase) =
        claseActions.actualizarClase(clase)

    fun actualizarClaseConAlumnos(clase: Clase, crossRefs: List<ClaseAlumnoCrossRef>) =
        claseActions.actualizarClaseConAlumnos(clase, crossRefs)

    fun marcarPagado(crossRef: ClaseAlumnoCrossRef) =
        claseActions.marcarPagado(crossRef)

    fun eliminarClase(clase: Clase) =
        claseActions.eliminarClase(clase)

    suspend fun validarSolapamiento(
        fecha: Long,
        horaInicio: String,
        horaFin: String,
        claseIdIgnorar: String = ""
    ): Boolean = claseActions.validarSolapamiento(fecha, horaInicio, horaFin, claseIdIgnorar)
}
