package com.nico.gestorclases.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.*
import com.nico.gestorclases.utils.DateUtils.toStartOfDayMillis
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClaseDialog(
    claseConAlumno: ClaseConAlumno? = null,
    alumnos: List<Alumno>,
    fechaInicial: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onConfirm: (Clase) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val esEdicion = claseConAlumno != null
    val clase = claseConAlumno?.clase

    // Estados del formulario
    var alumnoSeleccionado by remember {
        mutableStateOf(
            if (esEdicion) alumnos.find { it.id == clase?.alumnoId } else alumnos.firstOrNull()
        )
    }
    var fechaSeleccionada by remember { mutableStateOf(fechaInicial) }
    var horaInicio by remember { mutableStateOf(clase?.horaInicio ?: "09:00") }
    var horaFin by remember { mutableStateOf(clase?.horaFin ?: "10:00") }
    var precio by remember {
        mutableStateOf(
            clase?.precioClase?.toString()
                ?: alumnoSeleccionado?.precioPorDefecto?.toString()
                ?: "0"
        )
    }
    var estadoClase by remember { mutableStateOf(clase?.estadoClase ?: EstadoClase.RESERVADA) }
    var estadoPago by remember { mutableStateOf(clase?.estadoPago ?: EstadoPago.PENDIENTE) }
    var notas by remember { mutableStateOf(clase?.notas ?: "") }

    var mostrarAlumnoDropdown by remember { mutableStateOf(false) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarDeleteConfirm by remember { mutableStateOf(false) }

    // Actualizar precio cuando cambia el alumno (solo en modo creación)
    LaunchedEffect(alumnoSeleccionado) {
        if (!esEdicion && alumnoSeleccionado != null) {
            precio = alumnoSeleccionado!!.precioPorDefecto.toString()
        }
    }

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
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (esEdicion) "Editar clase" else "Nueva clase")
                if (esEdicion && onDelete != null) {
                    IconButton(onClick = { mostrarDeleteConfirm = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de alumno
                if (alumnos.isEmpty()) {
                    Text(
                        "No hay alumnos creados. Creá un alumno primero.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = mostrarAlumnoDropdown,
                        onExpandedChange = { mostrarAlumnoDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = alumnoSeleccionado?.nombreCompleto ?: "Seleccioná un alumno",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Alumno") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostrarAlumnoDropdown) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = mostrarAlumnoDropdown,
                            onDismissRequest = { mostrarAlumnoDropdown = false }
                        ) {
                            alumnos.forEach { alumno ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(alumno.nombreCompleto, fontWeight = FontWeight.Medium)
                                            Text(alumno.nivelEducativo.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        alumnoSeleccionado = alumno
                                        mostrarAlumnoDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Fecha
                OutlinedTextField(
                    value = "${fechaSeleccionada.dayOfMonth}/${fechaSeleccionada.monthValue}/${fechaSeleccionada.year}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { mostrarDatePicker = true }) { Text("Cambiar") }
                    }
                )

                // Horario
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = horaInicio,
                        onValueChange = { horaInicio = it },
                        label = { Text("Inicio") },
                        placeholder = { Text("09:00") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = horaFin,
                        onValueChange = { horaFin = it },
                        label = { Text("Fin") },
                        placeholder = { Text("10:00") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Precio
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio de la clase ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Estado de la clase
                Text("Estado de la clase", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    EstadoClase.entries.forEach { estado ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = estadoClase == estado,
                                onClick = { estadoClase = estado }
                            )
                            Text(estado.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Estado de pago
                Text("Estado del pago", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoPago.entries.forEach { ep ->
                        FilterChip(
                            selected = estadoPago == ep,
                            onClick = { estadoPago = ep },
                            label = { Text(ep.displayName) }
                        )
                    }
                }

                // Notas
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
                    val alumno = alumnoSeleccionado ?: return@Button
                    val precioDouble = precio.toDoubleOrNull() ?: 0.0
                    val nuevaClase = Clase(
                        id = clase?.id ?: 0,
                        alumnoId = alumno.id,
                        fecha = fechaSeleccionada.toStartOfDayMillis(),
                        horaInicio = horaInicio,
                        horaFin = horaFin,
                        precioClase = precioDouble,
                        estadoClase = estadoClase,
                        estadoPago = estadoPago,
                        notas = notas
                    )
                    onConfirm(nuevaClase)
                },
                enabled = alumnoSeleccionado != null
            ) {
                Text(if (esEdicion) "Guardar" else "Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
