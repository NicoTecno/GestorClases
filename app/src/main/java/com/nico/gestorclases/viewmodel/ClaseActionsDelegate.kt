package com.nico.gestorclases.viewmodel

import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.EstadoPago
import com.nico.gestorclases.data.repository.ClaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Delegado que centraliza las operaciones de escritura sobre [Clase] y [ClaseAlumnoCrossRef].
 *
 * **Por qué Delegate y no herencia:**
 * - [HomeViewModel], [CalendarViewModel] y [StudentsViewModel] tienen responsabilidades
 *   distintas de estado (fecha hoy vs mes vs alumno seleccionado). Una clase base común
 *   mezclaría esas responsabilidades.
 * - El patrón Delegate permite componer el comportamiento sin acoplamiento de herencia.
 *   Cada ViewModel crea su instancia propia y delega explícitamente.
 *
 * **Uso:**
 * ```kotlin
 * private val claseActions = ClaseActionsDelegate(claseRepository, viewModelScope)
 *
 * fun marcarPagado(crossRef: ClaseAlumnoCrossRef) = claseActions.marcarPagado(crossRef)
 * ```
 */
class ClaseActionsDelegate(
    private val claseRepository: ClaseRepository,
    private val scope: CoroutineScope
) {

    fun agregarClase(clase: Clase, crossRefs: List<ClaseAlumnoCrossRef>) = scope.launch {
        claseRepository.insertarClaseConAlumnos(clase, crossRefs)
    }

    /** Actualiza solo el evento (estadoClase, fecha, horas, notas). */
    fun actualizarClase(clase: Clase) = scope.launch {
        claseRepository.actualizarClase(clase)
    }

    /** Actualiza la clase y reemplaza todos sus participantes. */
    fun actualizarClaseConAlumnos(clase: Clase, crossRefs: List<ClaseAlumnoCrossRef>) =
        scope.launch {
            claseRepository.actualizarClaseConAlumnos(clase, crossRefs)
        }

    /** Marca el pago de un participante específico como PAGADA. */
    fun marcarPagado(crossRef: ClaseAlumnoCrossRef) = scope.launch {
        claseRepository.actualizarParticipante(crossRef.copy(estadoPago = EstadoPago.PAGADA))
    }

    fun eliminarClase(clase: Clase) = scope.launch {
        claseRepository.eliminarClase(clase)
    }

    /**
     * Retorna `true` si el horario propuesto se solapa con clases existentes.
     * Es suspend porque hace una query a Room; debe llamarse desde una corrutina.
     */
    suspend fun validarSolapamiento(
        fecha: Long,
        horaInicio: String,
        horaFin: String,
        claseIdIgnorar: String = ""
    ): Boolean = claseRepository.haySolapamientoDeHorario(fecha, horaInicio, horaFin, claseIdIgnorar)
}
