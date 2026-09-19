package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.ClaseConAlumnos
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.ClaseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModel(
    private val alumnoRepository: AlumnoRepository,
    claseRepository: ClaseRepository
) : ViewModel() {

    /** Delegado con toda la lógica de escritura sobre clases. */
    private val claseActions = ClaseActionsDelegate(claseRepository, viewModelScope)

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _alumnoSeleccionado = MutableStateFlow<Alumno?>(null)
    val alumnoSeleccionado: StateFlow<Alumno?> = _alumnoSeleccionado.asStateFlow()

    val alumnosFiltrados: StateFlow<List<Alumno>> = _busqueda
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) alumnoRepository.todosLosAlumnos
            else alumnoRepository.buscarAlumnos(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clasesDelAlumnoSeleccionado: StateFlow<List<ClaseConAlumnos>> =
        _alumnoSeleccionado.flatMapLatest { alumno ->
            if (alumno != null) claseRepository.getClasesDeAlumno(alumno.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Gestión de alumnos ────────────────────────────────────────────────────

    fun actualizarBusqueda(query: String) { _busqueda.value = query }

    fun seleccionarAlumno(alumno: Alumno?) { _alumnoSeleccionado.value = alumno }

    fun agregarAlumno(alumno: Alumno) = viewModelScope.launch {
        alumnoRepository.insertarAlumno(alumno)
    }

    fun actualizarAlumno(alumno: Alumno) = viewModelScope.launch {
        alumnoRepository.actualizarAlumno(alumno)
    }

    fun eliminarAlumno(alumno: Alumno) = viewModelScope.launch {
        alumnoRepository.eliminarAlumno(alumno)
    }

    // ── Delegación explícita de acciones sobre clases ─────────────────────────

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
        claseIdIgnorar: Int = 0
    ): Boolean = claseActions.validarSolapamiento(fecha, horaInicio, horaFin, claseIdIgnorar)
}
