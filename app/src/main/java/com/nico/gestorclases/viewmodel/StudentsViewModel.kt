package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.data.model.ClaseConAlumno
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.ClaseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModel(
    private val alumnoRepository: AlumnoRepository,
    private val claseRepository: ClaseRepository
) : ViewModel() {

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _alumnoSeleccionado = MutableStateFlow<Alumno?>(null)
    val alumnoSeleccionado: StateFlow<Alumno?> = _alumnoSeleccionado.asStateFlow()

    val alumnosFiltrados: StateFlow<List<Alumno>> = _busqueda
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                alumnoRepository.todosLosAlumnos
            } else {
                alumnoRepository.buscarAlumnos(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clasesDelAlumnoSeleccionado: StateFlow<List<ClaseConAlumno>> =
        _alumnoSeleccionado.flatMapLatest { alumno ->
            if (alumno != null) {
                claseRepository.getClasesDeAlumno(alumno.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun actualizarBusqueda(query: String) {
        _busqueda.value = query
    }

    fun seleccionarAlumno(alumno: Alumno?) {
        _alumnoSeleccionado.value = alumno
    }

    fun agregarAlumno(alumno: Alumno) = viewModelScope.launch {
        alumnoRepository.insertarAlumno(alumno)
    }

    fun actualizarAlumno(alumno: Alumno) = viewModelScope.launch {
        alumnoRepository.actualizarAlumno(alumno)
    }

    fun eliminarAlumno(alumno: Alumno) = viewModelScope.launch {
        alumnoRepository.eliminarAlumno(alumno)
    }
}
