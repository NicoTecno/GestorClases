package com.nico.gestorclases.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.ClaseConAlumno
import com.nico.gestorclases.ui.components.ClaseCard
import com.nico.gestorclases.ui.components.EmptyState
import com.nico.gestorclases.ui.dialogs.AddEditClaseDialog
import com.nico.gestorclases.utils.DateUtils.toDisplayString
import com.nico.gestorclases.viewmodel.HomeViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val clasesDeHoy by viewModel.clasesDeHoy.collectAsState()
    val alumnos by viewModel.todosLosAlumnos.collectAsState()
    val hoy = LocalDate.now()

    var mostrarDialogoAdd by remember { mutableStateOf(false) }
    var claseAEditar by remember { mutableStateOf<ClaseConAlumno?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Clases de Hoy",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            hoy.toDisplayString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        if (clasesDeHoy.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.EventBusy,
                title = "No hay clases para hoy",
                subtitle = "Tus clases programadas para el día de hoy aparecerán acá.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clasesDeHoy, key = { it.clase.id }) { claseConAlumno ->
                    ClaseCard(
                        claseConAlumno = claseConAlumno,
                        onClick = { claseAEditar = claseConAlumno }
                    )
                }
            }
        }
    }

    if (mostrarDialogoAdd) {
        AddEditClaseDialog(
            alumnos = alumnos,
            fechaInicial = hoy,
            onDismiss = { mostrarDialogoAdd = false },
            onConfirm = { nuevaClase ->
                viewModel.agregarClase(nuevaClase)
                mostrarDialogoAdd = false
            }
        )
    }

    claseAEditar?.let { cxa ->
        AddEditClaseDialog(
            claseConAlumno = cxa,
            alumnos = alumnos,
            fechaInicial = hoy, // no cambia la fecha original del dialogo a menos que el usuario lo haga
            onDismiss = { claseAEditar = null },
            onConfirm = { claseActualizada ->
                viewModel.actualizarClase(claseActualizada)
                claseAEditar = null
            },
            onDelete = {
                viewModel.eliminarClase(cxa.clase)
                claseAEditar = null
            }
        )
    }
}
