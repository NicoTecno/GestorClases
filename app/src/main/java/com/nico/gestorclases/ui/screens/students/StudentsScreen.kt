package com.nico.gestorclases.ui.screens.students

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.ui.components.EmptyState
import com.nico.gestorclases.ui.components.NivelBadge
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
            // Buscador
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
                    subtitle = if (busqueda.isBlank()) "Aún no tenés alumnos registrados. Creá uno para empezar."
                               else "No se encontraron alumnos con esa búsqueda.",
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

@Composable
fun AlumnoCard(alumno: Alumno, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con iniciales
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = alumno.iniciales,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alumno.nombreCompleto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                NivelBadge(label = alumno.nivelEducativo.displayName)
            }
        }
    }
}
