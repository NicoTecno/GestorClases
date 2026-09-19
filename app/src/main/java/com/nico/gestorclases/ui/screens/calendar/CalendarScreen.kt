package com.nico.gestorclases.ui.screens.calendar

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.ClaseConAlumnos
import com.nico.gestorclases.data.model.EstadoClase
import com.nico.gestorclases.ui.components.ClaseCard
import com.nico.gestorclases.ui.components.EmptyState
import com.nico.gestorclases.ui.dialogs.AddEditClaseDialog
import com.nico.gestorclases.ui.dialogs.CierreDelMesDialog
import com.nico.gestorclases.utils.DateUtils.toDisplayString
import com.nico.gestorclases.utils.ExcelExportService
import com.nico.gestorclases.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mesActual by viewModel.mesActual.collectAsState()
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    val diasConClases by viewModel.diasConClases.collectAsState()
    val clasesDelDia by viewModel.clasesDelDia.collectAsState()
    val clasesDelMes by viewModel.clasesDelMes.collectAsState()
    val cierreInfo by viewModel.cierreDelMes.collectAsState()
    val alumnos by viewModel.todosLosAlumnos.collectAsState()

    var mostrarCierre by remember { mutableStateOf(false) }
    var mostrarDialogoAdd by remember { mutableStateOf(false) }
    var claseAEditar by remember { mutableStateOf<ClaseConAlumnos?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario", fontWeight = FontWeight.Bold) },
                actions = {
                    // Botón exportar Excel — el I/O pesado corre en Dispatchers.IO dentro
                    // de generarArchivoExcel; compartirArchivo vuelve al Main dispatcher.
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val file = ExcelExportService.generarArchivoExcel(
                                        context = context,
                                        clasesDelMes = clasesDelMes,
                                        mesAnio = mesActual.toString()
                                    )
                                    ExcelExportService.compartirArchivo(context, file)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error al generar Excel: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    ) {
                        // Ícono cambiado de Share a Description para comunicar "exportar documento"
                        Icon(Icons.Filled.Description, contentDescription = "Exportar a Excel")
                    }
                    Button(
                        onClick = { mostrarCierre = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Functions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Cierre")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogoAdd = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Agregar Clase")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Cabecera de navegación del mes ────────────────────────────────
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.irMesAnterior() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior")
                    }
                    Text(
                        text = mesActual.toDisplayString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.irMesSiguiente() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente")
                    }
                }
            }

            // ── Calendario ────────────────────────────────────────────────────
            item(key = "calendar") {
                CustomCalendar(
                    mesActual = mesActual,
                    fechaSeleccionada = fechaSeleccionada,
                    diasConClases = diasConClases,
                    onDateSelected = { viewModel.seleccionarFecha(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            // ── Título de clases del día ──────────────────────────────────────
            item(key = "title") {
                Text(
                    text = "Clases del ${fechaSeleccionada.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // ── Clases del día ────────────────────────────────────────────────
            if (clasesDelDia.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.EventBusy,
                        title = "Día libre",
                        subtitle = "No hay clases registradas para esta fecha.",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(clasesDelDia, key = { it.clase.id }) { claseConAlumnos ->
                    ClaseCard(
                        claseConAlumnos = claseConAlumnos,
                        onEdit = { claseAEditar = claseConAlumnos },
                        onMarkDada = {
                            viewModel.actualizarClase(
                                claseConAlumnos.clase.copy(estadoClase = EstadoClase.DADA)
                            )
                        },
                        onMarkPagado = { crossRef ->
                            viewModel.marcarPagado(crossRef)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // ── Diálogos (fuera del Scaffold para mantener consistencia) ──────────────

    if (mostrarCierre && cierreInfo != null) {
        CierreDelMesDialog(
            info = cierreInfo!!,
            onDismiss = { mostrarCierre = false }
        )
    }

    if (mostrarDialogoAdd) {
        AddEditClaseDialog(
            alumnos = alumnos,
            fechaInicial = fechaSeleccionada,
            onDismiss = { mostrarDialogoAdd = false },
            onConfirm = { resultado ->
                viewModel.agregarClase(resultado.clase, resultado.crossRefs)
                mostrarDialogoAdd = false
            },
            validarSolapamiento = { fecha, inicio, fin, ignorarId ->
                viewModel.validarSolapamiento(fecha, inicio, fin, ignorarId)
            }
        )
    }

    claseAEditar?.let { cxa ->
        AddEditClaseDialog(
            claseConAlumnos = cxa,
            alumnos = alumnos,
            fechaInicial = fechaSeleccionada,
            onDismiss = { claseAEditar = null },
            onConfirm = { resultado ->
                viewModel.actualizarClaseConAlumnos(resultado.clase, resultado.crossRefs)
                claseAEditar = null
            },
            onDelete = {
                viewModel.eliminarClase(cxa.clase)
                claseAEditar = null
            },
            validarSolapamiento = { fecha, inicio, fin, ignorarId ->
                viewModel.validarSolapamiento(fecha, inicio, fin, ignorarId)
            }
        )
    }
}

// ── Calendario personalizado ──────────────────────────────────────────────────

@Composable
fun CustomCalendar(
    mesActual: YearMonth,
    fechaSeleccionada: LocalDate,
    diasConClases: Set<Int>,
    onDateSelected: (LocalDate) -> Unit
) {
    val diasDeLaSemana = listOf("L", "M", "M", "J", "V", "S", "D")
    val primerDiaDelMes = mesActual.atDay(1)
    val ultimoDiaDelMes = mesActual.atEndOfMonth()
    val celdasVaciasAlInicio = primerDiaDelMes.dayOfWeek.value - 1
    val totalDias = ultimoDiaDelMes.dayOfMonth

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            diasDeLaSemana.forEach { dia ->
                Text(
                    text = dia,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(celdasVaciasAlInicio) {
                Spacer(modifier = Modifier.aspectRatio(1f))
            }
            items(totalDias) { index ->
                val dia = index + 1
                val fecha = mesActual.atDay(dia)
                val esSeleccionado = fecha == fechaSeleccionada
                val tieneClases = diasConClases.contains(dia)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            if (esSeleccionado) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable { onDateSelected(fecha) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dia.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                            color = if (esSeleccionado) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (tieneClases) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (esSeleccionado) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                        } else {
                            Spacer(Modifier.size(6.dp))
                        }
                    }
                }
            }
        }
    }
}
