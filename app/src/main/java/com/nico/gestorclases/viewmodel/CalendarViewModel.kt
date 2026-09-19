package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.ClaseConAlumnos
import com.nico.gestorclases.data.model.EstadoClase
import com.nico.gestorclases.data.model.EstadoPago
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.ClaseRepository
import com.nico.gestorclases.utils.DateUtils.toEndMillis
import com.nico.gestorclases.utils.DateUtils.toLocalDate
import com.nico.gestorclases.utils.DateUtils.toStartMillis
import com.nico.gestorclases.utils.DateUtils.toStartOfDayMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth

data class AlumnoConDeuda(
    val alumno: Alumno,
    val montoPendiente: Double,
    val cantidadClases: Int,
    val clases: List<ClaseConAlumnos>
)

data class CierreDelMesInfo(
    val mesAnio: String,
    val clasesReservadas: Int,
    val clasesDadas: Int,
    val canceladasConTiempo: Int,
    val canceladasMismoDia: Int,
    val montoTotalFacturable: Double,
    val montoCobrado: Double,
    val montoPendiente: Double,
    val alumnosConDeuda: List<AlumnoConDeuda>
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val alumnoRepository: AlumnoRepository,
    claseRepository: ClaseRepository
) : ViewModel() {

    /** Delegado con toda la lógica de escritura sobre clases. */
    private val claseActions = ClaseActionsDelegate(claseRepository, viewModelScope)

    private val _mesActual = MutableStateFlow(YearMonth.now())
    val mesActual: StateFlow<YearMonth> = _mesActual.asStateFlow()

    private val _fechaSeleccionada = MutableStateFlow(LocalDate.now())
    val fechaSeleccionada: StateFlow<LocalDate> = _fechaSeleccionada.asStateFlow()

    val todosLosAlumnos: StateFlow<List<Alumno>> =
        alumnoRepository.todosLosAlumnos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clasesDelMes: StateFlow<List<ClaseConAlumnos>> = _mesActual.flatMapLatest { mes ->
        claseRepository.getClasesDelMes(mes.toStartMillis(), mes.toEndMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clasesDelDia: StateFlow<List<ClaseConAlumnos>> = combine(
        _fechaSeleccionada,
        clasesDelMes
    ) { fecha, clases ->
        val fechaMillis = fecha.toStartOfDayMillis()
        clases.filter { it.clase.fecha == fechaMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val diasConClases: StateFlow<Set<Int>> = clasesDelMes.map { clases ->
        clases.map { it.clase.fecha.toLocalDate().dayOfMonth }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val cierreDelMes: StateFlow<CierreDelMesInfo?> = clasesDelMes.map { clases ->
        calcularCierre(clases)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Navegación de mes ─────────────────────────────────────────────────────

    fun irMesAnterior() { _mesActual.value = _mesActual.value.minusMonths(1) }

    fun irMesSiguiente() { _mesActual.value = _mesActual.value.plusMonths(1) }

    fun seleccionarFecha(fecha: LocalDate) {
        _fechaSeleccionada.value = fecha
        val nuevoMes = YearMonth.of(fecha.year, fecha.month)
        if (nuevoMes != _mesActual.value) _mesActual.value = nuevoMes
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
        claseIdIgnorar: Int = 0
    ): Boolean = claseActions.validarSolapamiento(fecha, horaInicio, horaFin, claseIdIgnorar)

    // ── Cierre del mes ────────────────────────────────────────────────────────

    private fun calcularCierre(clases: List<ClaseConAlumnos>): CierreDelMesInfo? {
        if (clases.isEmpty()) return null

        val reservadas = clases.count { it.clase.estadoClase == EstadoClase.RESERVADA }
        val dadas = clases.count { it.clase.estadoClase == EstadoClase.DADA }
        val canceladasConTiempo = clases.count { it.clase.estadoClase == EstadoClase.CANCELADA_CON_TIEMPO }
        val canceladasMismoDia = clases.count { it.clase.estadoClase == EstadoClase.CANCELADA_MISMO_DIA }

        // Las facturables son DADAS y CANCELADAS_MISMO_DIA
        val facturables = clases.filter {
            it.clase.estadoClase == EstadoClase.DADA ||
                    it.clase.estadoClase == EstadoClase.CANCELADA_MISMO_DIA
        }

        val montoTotal = facturables.sumOf { it.precioTotal }
        val montoCobrado = facturables.sumOf { clase ->
            clase.participantes
                .filter { it.estadoPago == EstadoPago.PAGADA }
                .sumOf { it.precioIndividual }
        }
        val montoPendiente = montoTotal - montoCobrado

        // Deudores: agrupar por alumno, considerando clases grupales
        val deudoresPorAlumno = mutableMapOf<Alumno, MutableList<ClaseConAlumnos>>()
        facturables.forEach { claseConAlumnos ->
            claseConAlumnos.participantes
                .filter { it.estadoPago == EstadoPago.PENDIENTE }
                .forEach { crossRef ->
                    val alumno = claseConAlumnos.alumnos.find { it.id == crossRef.alumnoId }
                    if (alumno != null) {
                        deudoresPorAlumno.getOrPut(alumno) { mutableListOf() }.add(claseConAlumnos)
                    }
                }
        }

        val deudores = deudoresPorAlumno.map { (alumno, clasesDeudoras) ->
            val montoPendienteAlumno = clasesDeudoras.sumOf { clase ->
                clase.participantes
                    .filter { it.alumnoId == alumno.id && it.estadoPago == EstadoPago.PENDIENTE }
                    .sumOf { it.precioIndividual }
            }
            AlumnoConDeuda(
                alumno = alumno,
                montoPendiente = montoPendienteAlumno,
                cantidadClases = clasesDeudoras.size,
                clases = clasesDeudoras
            )
        }.sortedByDescending { it.montoPendiente }

        return CierreDelMesInfo(
            mesAnio = _mesActual.value.toString(),
            clasesReservadas = reservadas,
            clasesDadas = dadas,
            canceladasConTiempo = canceladasConTiempo,
            canceladasMismoDia = canceladasMismoDia,
            montoTotalFacturable = montoTotal,
            montoCobrado = montoCobrado,
            montoPendiente = montoPendiente,
            alumnosConDeuda = deudores
        )
    }
}
