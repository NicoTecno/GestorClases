package com.nico.gestorclases.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.*
import com.nico.gestorclases.utils.DateUtils.toMinutes
import com.nico.gestorclases.utils.DateUtils.toStartOfDayMillis
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Resultado que se emite al confirmar el diálogo.
 * Contiene la [Clase] (evento) y la lista de [ClaseAlumnoCrossRef] (participantes).
 */
data class ResultadoClaseDialog(
    val clase: Clase,
    val crossRefs: List<ClaseAlumnoCrossRef>
)

/**
 * Diálogo para crear o editar una clase (individual o grupal).
 *
 * Está descompuesto en sub-composables para mantener la legibilidad:
 * - [AlumnosSelectorSection]: slots de selección de alumnos (solo en creación).
 * - [PreciosPagosSection]: precios y estados de pago por alumno.
 * - [FechaSelectorField]: campo de fecha con DatePicker.
 * - [HorarioSelectorRow]: fila de inicio/fin con TimePicker.
 * - [EstadoClaseSection]: selector de estado (solo en edición).
 * - [ErroresHorarioSection]: mensajes de error de validación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClaseDialog(
    claseConAlumnos: ClaseConAlumnos? = null,
    alumnos: List<Alumno>,
    fechaInicial: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onConfirm: (ResultadoClaseDialog) -> Unit,
    onDelete: (() -> Unit)? = null,
    validarSolapamiento: suspend (fecha: Long, inicio: String, fin: String, ignorarId: Int) -> Boolean = { _, _, _, _ -> false }
) {
    val esEdicion = claseConAlumnos != null
    val clase = claseConAlumnos?.clase
    val coroutineScope = rememberCoroutineScope()

    // ── Estado del formulario ─────────────────────────────────────────────────

    var slotsAlumnos by remember {
        mutableStateOf(
            if (esEdicion) claseConAlumnos!!.alumnos.toList() else listOf<Alumno?>(null)
        )
    }
    var preciosPorAlumno by remember {
        mutableStateOf(
            if (esEdicion) claseConAlumnos!!.participantes.associate { it.alumnoId to it.precioIndividual.toString() }
            else emptyMap<Int, String>()
        )
    }
    var estadosPagoPorAlumno by remember {
        mutableStateOf(
            if (esEdicion) claseConAlumnos!!.participantes.associate { it.alumnoId to it.estadoPago }
            else emptyMap<Int, EstadoPago>()
        )
    }

    var fechaSeleccionada by remember { mutableStateOf(fechaInicial) }
    var horaInicio by remember { mutableStateOf(clase?.horaInicio ?: "09:00") }
    var horaFin by remember { mutableStateOf(clase?.horaFin ?: "10:00") }
    var estadoClase by remember { mutableStateOf(clase?.estadoClase ?: EstadoClase.RESERVADA) }
    var notas by remember { mutableStateOf(clase?.notas ?: "") }

    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarTimePickerInicio by remember { mutableStateOf(false) }
    var mostrarTimePickerFin by remember { mutableStateOf(false) }
    var mostrarDeleteConfirm by remember { mutableStateOf(false) }
    var errorSolapamiento by remember { mutableStateOf(false) }
    var errorHorario by remember { mutableStateOf(false) }

    // ── Helpers de slots ──────────────────────────────────────────────────────

    fun resetErrores() { errorSolapamiento = false; errorHorario = false }

    fun updateSlot(index: Int, nuevoAlumno: Alumno) {
        val oldAlumno = slotsAlumnos[index]
        val nuevaLista = slotsAlumnos.toMutableList().also { it[index] = nuevoAlumno }
        slotsAlumnos = nuevaLista
        if (oldAlumno != null && oldAlumno != nuevoAlumno && !nuevaLista.contains(oldAlumno)) {
            preciosPorAlumno = preciosPorAlumno - oldAlumno.id
            estadosPagoPorAlumno = estadosPagoPorAlumno - oldAlumno.id
        }
        if (!preciosPorAlumno.containsKey(nuevoAlumno.id)) {
            preciosPorAlumno = preciosPorAlumno + (nuevoAlumno.id to nuevoAlumno.precioPorDefecto.toString())
        }
    }

    fun removeSlot(index: Int) {
        val oldAlumno = slotsAlumnos[index]
        val nuevaLista = slotsAlumnos.toMutableList().also { it.removeAt(index) }
        slotsAlumnos = nuevaLista
        if (oldAlumno != null && !nuevaLista.contains(oldAlumno)) {
            preciosPorAlumno = preciosPorAlumno - oldAlumno.id
            estadosPagoPorAlumno = estadosPagoPorAlumno - oldAlumno.id
        }
    }

    // ── Pickers de fecha y hora ───────────────────────────────────────────────

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaSeleccionada.toStartOfDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        fechaSeleccionada = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    mostrarDatePicker = false
                    resetErrores()
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (mostrarTimePickerInicio) {
        val parts = horaInicio.split(":")
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true
        )
        TimePickerDialog(
            onDismissRequest = { mostrarTimePickerInicio = false },
            confirmButton = {
                TextButton(onClick = {
                    horaInicio = "${timeState.hour.toString().padStart(2, '0')}:${timeState.minute.toString().padStart(2, '0')}"
                    mostrarTimePickerInicio = false
                    resetErrores()
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarTimePickerInicio = false }) { Text("Cancelar") }
            }
        ) { TimePicker(state = timeState) }
    }

    if (mostrarTimePickerFin) {
        val parts = horaFin.split(":")
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 10,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true
        )
        TimePickerDialog(
            onDismissRequest = { mostrarTimePickerFin = false },
            confirmButton = {
                TextButton(onClick = {
                    horaFin = "${timeState.hour.toString().padStart(2, '0')}:${timeState.minute.toString().padStart(2, '0')}"
                    mostrarTimePickerFin = false
                    resetErrores()
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarTimePickerFin = false }) { Text("Cancelar") }
            }
        ) { TimePicker(state = timeState) }
    }

    if (mostrarDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { mostrarDeleteConfirm = false },
            title = { Text("Eliminar clase") },
            text = { Text("¿Estás seguro que querés eliminar esta clase? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete?.invoke() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo principal ─────────────────────────────────────────────────────

    val alumnosFinales = slotsAlumnos.filterNotNull().distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            DialogTitleRow(
                esEdicion = esEdicion,
                onDelete = onDelete,
                onShowDeleteConfirm = { mostrarDeleteConfirm = true }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AlumnosSelectorSection(
                    esEdicion = esEdicion,
                    alumnos = alumnos,
                    slotsAlumnos = slotsAlumnos,
                    alumnosFinales = alumnosFinales,
                    onUpdateSlot = { index, alumno -> updateSlot(index, alumno) },
                    onRemoveSlot = { index -> removeSlot(index) },
                    onAddSlot = { slotsAlumnos = slotsAlumnos + null }
                )

                PreciosPagosSection(
                    esEdicion = esEdicion,
                    alumnosFinales = alumnosFinales,
                    preciosPorAlumno = preciosPorAlumno,
                    estadosPagoPorAlumno = estadosPagoPorAlumno,
                    onPrecioChange = { alumnoId, valor ->
                        preciosPorAlumno = preciosPorAlumno + (alumnoId to valor)
                    },
                    onEstadoPagoChange = { alumnoId, estado ->
                        estadosPagoPorAlumno = estadosPagoPorAlumno + (alumnoId to estado)
                    }
                )

                FechaSelectorField(
                    fechaSeleccionada = fechaSeleccionada,
                    onClickFecha = { mostrarDatePicker = true }
                )

                HorarioSelectorRow(
                    horaInicio = horaInicio,
                    horaFin = horaFin,
                    tieneError = errorSolapamiento || errorHorario,
                    onClickInicio = { mostrarTimePickerInicio = true },
                    onClickFin = { mostrarTimePickerFin = true }
                )

                ErroresHorarioSection(
                    errorHorario = errorHorario,
                    errorSolapamiento = errorSolapamiento
                )

                if (esEdicion) {
                    EstadoClaseSection(
                        estadoClase = estadoClase,
                        onEstadoChange = { estadoClase = it }
                    )
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (alumnosFinales.isEmpty()) return@Button

                    // Reutiliza String.toMinutes() de DateUtils en lugar de duplicar la lógica
                    if (horaFin.toMinutes() <= horaInicio.toMinutes()) {
                        errorHorario = true
                        return@Button
                    }
                    errorHorario = false

                    coroutineScope.launch {
                        val fechaMillis = fechaSeleccionada.toStartOfDayMillis()
                        val haySolape = validarSolapamiento(fechaMillis, horaInicio, horaFin, clase?.id ?: 0)
                        if (haySolape) {
                            errorSolapamiento = true
                            return@launch
                        }
                        errorSolapamiento = false

                        val nuevaClase = Clase(
                            id = clase?.id ?: 0,
                            fecha = fechaMillis,
                            horaInicio = horaInicio,
                            horaFin = horaFin,
                            estadoClase = if (esEdicion) estadoClase else EstadoClase.RESERVADA,
                            notas = notas
                        )
                        val crossRefs = alumnosFinales.map { alumno ->
                            ClaseAlumnoCrossRef(
                                claseId = clase?.id ?: 0,
                                alumnoId = alumno.id,
                                precioIndividual = preciosPorAlumno[alumno.id]?.toDoubleOrNull()
                                    ?: alumno.precioPorDefecto,
                                estadoPago = estadosPagoPorAlumno[alumno.id] ?: EstadoPago.PENDIENTE
                            )
                        }
                        onConfirm(ResultadoClaseDialog(nuevaClase, crossRefs))
                    }
                },
                enabled = alumnosFinales.isNotEmpty()
            ) {
                Text(if (esEdicion) "Guardar" else "Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun DialogTitleRow(
    esEdicion: Boolean,
    onDelete: (() -> Unit)?,
    onShowDeleteConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (esEdicion) "Editar clase" else "Nueva clase")
        if (esEdicion && onDelete != null) {
            IconButton(onClick = onShowDeleteConfirm) {
                Icon(Icons.Filled.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AlumnosSelectorSection(
    esEdicion: Boolean,
    alumnos: List<Alumno>,
    slotsAlumnos: List<Alumno?>,
    alumnosFinales: List<Alumno>,
    onUpdateSlot: (Int, Alumno) -> Unit,
    onRemoveSlot: (Int) -> Unit,
    onAddSlot: () -> Unit
) {
    Text("Alumnos", style = MaterialTheme.typography.labelLarge)

    when {
        alumnos.isEmpty() -> {
            Text(
                "No hay alumnos creados. Creá un alumno primero.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        esEdicion -> {
            Text(
                text = alumnosFinales.joinToString(", ") { it.nombreCompleto },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slotsAlumnos.forEachIndexed { index, selected ->
                    SearchableAlumnoDropdown(
                        alumnos = alumnos,
                        selectedAlumno = selected,
                        onAlumnoSelected = { onUpdateSlot(index, it) },
                        onRemove = if (slotsAlumnos.size > 1) { { onRemoveSlot(index) } } else null
                    )
                }
                TextButton(onClick = onAddSlot) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar otro alumno")
                }
            }
            if (alumnosFinales.isEmpty()) {
                Text(
                    "Seleccioná al menos un alumno.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PreciosPagosSection(
    esEdicion: Boolean,
    alumnosFinales: List<Alumno>,
    preciosPorAlumno: Map<Int, String>,
    estadosPagoPorAlumno: Map<Int, EstadoPago>,
    onPrecioChange: (Int, String) -> Unit,
    onEstadoPagoChange: (Int, EstadoPago) -> Unit
) {
    if (alumnosFinales.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (esEdicion) "Precios y estado de pago" else "Precio por alumno",
            style = MaterialTheme.typography.labelLarge
        )
        alumnosFinales.forEach { alumno ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = alumno.nombre,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = preciosPorAlumno[alumno.id] ?: "0",
                        onValueChange = { onPrecioChange(alumno.id, it) },
                        label = { Text("Precio") },
                        modifier = Modifier.width(130.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("$") }
                    )
                }
                if (esEdicion) {
                    val estadoPago = estadosPagoPorAlumno[alumno.id] ?: EstadoPago.PENDIENTE
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        EstadoPago.entries.forEach { ep ->
                            FilterChip(
                                selected = estadoPago == ep,
                                onClick = { onEstadoPagoChange(alumno.id, ep) },
                                label = { Text(ep.displayName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FechaSelectorField(
    fechaSeleccionada: LocalDate,
    onClickFecha: () -> Unit
) {
    Box(modifier = Modifier.clickable { onClickFecha() }) {
        OutlinedTextField(
            value = "${fechaSeleccionada.dayOfMonth}/${fechaSeleccionada.monthValue}/${fechaSeleccionada.year}",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Fecha") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                Icon(Icons.Filled.CalendarToday, contentDescription = "Cambiar fecha")
            }
        )
    }
}

@Composable
private fun HorarioSelectorRow(
    horaInicio: String,
    horaFin: String,
    tieneError: Boolean,
    onClickInicio: () -> Unit,
    onClickFin: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f).clickable { onClickInicio() }) {
            OutlinedTextField(
                value = horaInicio,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Inicio") },
                modifier = Modifier.fillMaxWidth(),
                isError = tieneError,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (tieneError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    disabledLabelColor = if (tieneError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = "Cambiar inicio") }
            )
        }
        Box(modifier = Modifier.weight(1f).clickable { onClickFin() }) {
            OutlinedTextField(
                value = horaFin,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Fin") },
                modifier = Modifier.fillMaxWidth(),
                isError = tieneError,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (tieneError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    disabledLabelColor = if (tieneError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = "Cambiar fin") }
            )
        }
    }
}

@Composable
private fun ErroresHorarioSection(errorHorario: Boolean, errorSolapamiento: Boolean) {
    AnimatedVisibility(visible = errorHorario) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Text(
                "La hora de fin debe ser posterior al inicio.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    AnimatedVisibility(visible = errorSolapamiento) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Text(
                "Ya existe una clase en ese horario.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EstadoClaseSection(
    estadoClase: EstadoClase,
    onEstadoChange: (EstadoClase) -> Unit
) {
    Text("Estado de la clase", style = MaterialTheme.typography.labelLarge)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        EstadoClase.entries.forEach { estado ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(selected = estadoClase == estado, onClick = { onEstadoChange(estado) })
                Text(estado.displayName, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ── Componentes de soporte ────────────────────────────────────────────────────

@Composable
fun TimePickerDialog(
    title: String = "Seleccionar hora",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { content() },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableAlumnoDropdown(
    alumnos: List<Alumno>,
    selectedAlumno: Alumno?,
    onAlumnoSelected: (Alumno) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(selectedAlumno?.nombreCompleto ?: "") }

    LaunchedEffect(selectedAlumno) {
        if (selectedAlumno != null) searchQuery = selectedAlumno.nombreCompleto
    }

    val filteredAlumnos = alumnos.filter {
        it.nombreCompleto.contains(searchQuery, ignoreCase = true)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; expanded = true },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                    .fillMaxWidth(),
                label = { Text("Buscar alumno") },
                singleLine = true,
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (filteredAlumnos.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No se encontraron alumnos") },
                        onClick = { expanded = false }
                    )
                } else {
                    filteredAlumnos.forEach { alumno ->
                        DropdownMenuItem(
                            text = { Text(alumno.nombreCompleto) },
                            onClick = {
                                searchQuery = alumno.nombreCompleto
                                expanded = false
                                onAlumnoSelected(alumno)
                            }
                        )
                    }
                }
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Quitar")
            }
        }
    }
}
