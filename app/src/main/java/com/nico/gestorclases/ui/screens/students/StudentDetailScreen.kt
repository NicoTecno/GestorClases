package com.nico.gestorclases.ui.screens.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.ui.components.ClaseCard
import com.nico.gestorclases.ui.components.EmptyState
import com.nico.gestorclases.ui.components.NivelBadge
import com.nico.gestorclases.ui.dialogs.AddEditAlumnoDialog
import com.nico.gestorclases.utils.DateUtils.toDisplayString
import com.nico.gestorclases.utils.DateUtils.toLocalDate
import com.nico.gestorclases.viewmodel.StudentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    viewModel: StudentsViewModel,
    onBack: () -> Unit
) {
    val alumno by viewModel.alumnoSeleccionado.collectAsState()
    val clases by viewModel.clasesDelAlumnoSeleccionado.collectAsState()

    var mostrarDialogoEdit by remember { mutableStateOf(false) }

    if (alumno == null) {
        onBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Alumno") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoEdit = true }) {
                        Icon(Icons.Filled.Edit, "Editar Alumno")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Perfil header
            StudentProfileHeader(alumno = alumno!!)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Historial de clases
            Text(
                text = "Historial de Clases",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (clases.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.EventBusy,
                    title = "Sin clases",
                    subtitle = "Este alumno aún no ha tomado ninguna clase.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clases, key = { it.clase.id }) { claseConAlumno ->
                        Column {
                            // Separador visual de fecha
                            Text(
                                text = claseConAlumno.clase.fecha.toLocalDate().toDisplayString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                            ClaseCard(
                                claseConAlumno = claseConAlumno,
                                onClick = { /* Podríamos abrir edición de clase desde acá en el futuro */ }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoEdit) {
        AddEditAlumnoDialog(
            alumno = alumno,
            onDismiss = { mostrarDialogoEdit = false },
            onConfirm = { alumnoEditado ->
                viewModel.actualizarAlumno(alumnoEditado)
                viewModel.seleccionarAlumno(alumnoEditado)
                mostrarDialogoEdit = false
            }
        )
    }
}

@Composable
private fun StudentProfileHeader(alumno: Alumno) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = alumno.iniciales,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = alumno.nombreCompleto,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(8.dp))
        
        NivelBadge(label = alumno.nivelEducativo.displayName)

        if (alumno.telefono.isNotBlank() || alumno.telefonoTutor.isNotBlank() || alumno.notas.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (alumno.telefono.isNotBlank()) {
                        ContactRow(Icons.Filled.PhoneAndroid, "Alumno: ${alumno.telefono}")
                    }
                    if (alumno.telefonoTutor.isNotBlank()) {
                        ContactRow(Icons.Filled.Phone, "Tutor: ${alumno.telefonoTutor}")
                    }
                    if (alumno.notas.isNotBlank()) {
                        Text(
                            text = "Notas: ${alumno.notas}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
