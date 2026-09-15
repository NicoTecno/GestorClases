package com.nico.gestorclases.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseConAlumno
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class AlumnoConDeuda(
    val alumno: Alumno,
    val montoPendiente: Double,
    val cantidadClases: Int
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
    private val claseRepository: ClaseRepository
) : ViewModel() {

    private val _mesActual = MutableStateFlow(YearMonth.now())
    val mesActual: StateFlow<YearMonth> = _mesActual.asStateFlow()

    private val _fechaSeleccionada = MutableStateFlow(LocalDate.now())
    val fechaSeleccionada: StateFlow<LocalDate> = _fechaSeleccionada.asStateFlow()

    val todosLosAlumnos: StateFlow<List<Alumno>> =
        alumnoRepository.todosLosAlumnos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clasesDelMes: StateFlow<List<ClaseConAlumno>> = _mesActual.flatMapLatest { mes ->
        claseRepository.getClasesDelMes(mes.toStartMillis(), mes.toEndMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clasesDelDia: StateFlow<List<ClaseConAlumno>> = combine(
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

    fun irMesAnterior() {
        _mesActual.value = _mesActual.value.minusMonths(1)
    }

    fun irMesSiguiente() {
        _mesActual.value = _mesActual.value.plusMonths(1)
    }

    fun seleccionarFecha(fecha: LocalDate) {
        _fechaSeleccionada.value = fecha
        // Si la fecha es de un mes diferente, navegar a ese mes
        val nuevoMes = YearMonth.of(fecha.year, fecha.month)
        if (nuevoMes != _mesActual.value) {
            _mesActual.value = nuevoMes
        }
    }

    fun agregarClase(clase: Clase) = viewModelScope.launch {
        claseRepository.insertarClase(clase)
    }

    fun actualizarClase(clase: Clase) = viewModelScope.launch {
        claseRepository.actualizarClase(clase)
    }

    fun eliminarClase(clase: Clase) = viewModelScope.launch {
        claseRepository.eliminarClase(clase)
    }

    private fun calcularCierre(clases: List<ClaseConAlumno>): CierreDelMesInfo? {
        if (clases.isEmpty()) return null

        val reservadas = clases.count { it.clase.estadoClase == EstadoClase.RESERVADA }
        val dadas = clases.count { it.clase.estadoClase == EstadoClase.DADA }
        val canceladasConTiempo = clases.count { it.clase.estadoClase == EstadoClase.CANCELADA_CON_TIEMPO }
        val canceladasMismoDia = clases.count { it.clase.estadoClase == EstadoClase.CANCELADA_MISMO_DIA }

        // Las facturables son DADAS y CANCELADAS_MISMO_DIA (el alumno paga igual)
        val facturables = clases.filter {
            it.clase.estadoClase == EstadoClase.DADA ||
                    it.clase.estadoClase == EstadoClase.CANCELADA_MISMO_DIA
        }

        val montoTotal = facturables.sumOf { it.clase.precioClase }
        val montoCobrado = facturables
            .filter { it.clase.estadoPago == EstadoPago.PAGADA }
            .sumOf { it.clase.precioClase }
        val montoPendiente = montoTotal - montoCobrado

        // Agrupar deudores por alumno
        val deudores = facturables
            .filter { it.clase.estadoPago == EstadoPago.PENDIENTE }
            .groupBy { it.alumno }
            .map { (alumno, clasesDeudoras) ->
                AlumnoConDeuda(
                    alumno = alumno,
                    montoPendiente = clasesDeudoras.sumOf { it.clase.precioClase },
                    cantidadClases = clasesDeudoras.size
                )
            }
            .sortedByDescending { it.montoPendiente }

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
