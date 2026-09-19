package com.nico.gestorclases.ui.screens.students

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.ui.components.AlumnoCard
import com.nico.gestorclases.ui.components.EmptyState
import com.nico.gestorclases.ui.dialogs.AddEditAlumnoDialog
import com.nico.gestorclases.viewmodel.StudentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    viewModel: StudentsViewModel,
    onNavigateToDetail: (Alumno) -> Unit
) {
    val busqueda by viewModel.busqueda.collectAsState()
    val alumnos by viewModel.alumnosFiltrados.collectAsState()

    var mostrarDialogoAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alumnos", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogoAdd = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Agregar Alumno")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { viewModel.actualizarBusqueda(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por nombre o apellido...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (alumnos.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.GroupOff,
                    title = "Sin resultados",
                    subtitle = if (busqueda.isBlank())
                        "Aún no tenés alumnos registrados. Creá uno para empezar."
                    else
                        "No se encontraron alumnos con esa búsqueda.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alumnos, key = { it.id }) { alumno ->
                        AlumnoCard(
                            alumno = alumno,
                            onClick = { onNavigateToDetail(alumno) }
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoAdd) {
        AddEditAlumnoDialog(
            onDismiss = { mostrarDialogoAdd = false },
            onConfirm = { nuevoAlumno ->
                viewModel.agregarAlumno(nuevoAlumno)
                mostrarDialogoAdd = false
            }
        )
    }
}
