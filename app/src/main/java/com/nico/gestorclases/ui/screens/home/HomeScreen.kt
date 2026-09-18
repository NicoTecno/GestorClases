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
import com.nico.gestorclases.data.model.ClaseConAlumnos
import com.nico.gestorclases.data.model.EstadoClase
import com.nico.gestorclases.ui.components.ClaseCard
import com.nico.gestorclases.ui.components.EmptyState
import com.nico.gestorclases.ui.dialogs.AddEditClaseDialog
import com.nico.gestorclases.utils.DateUtils.toDisplayString
import com.nico.gestorclases.viewmodel.HomeViewModel
import java.time.LocalDate

import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.platform.LocalContext
import com.nico.gestorclases.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, authViewModel: AuthViewModel? = null) {
    val clasesDeHoy by viewModel.clasesDeHoy.collectAsState()
    val alumnos by viewModel.todosLosAlumnos.collectAsState()
    val currentUser by (authViewModel?.currentUser ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
    val hoy = LocalDate.now()
    val context = LocalContext.current

    var mostrarDialogoAdd by remember { mutableStateOf(false) }
    var claseAEditar by remember { mutableStateOf<ClaseConAlumnos?>(null) }
    var mostrarMenuPerfil by remember { mutableStateOf(false) }

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
                },
                actions = {
                    IconButton(onClick = { mostrarMenuPerfil = true }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil", modifier = Modifier.size(32.dp))
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

    // ... código de contenido principal

    if (mostrarMenuPerfil) {
        AlertDialog(
            onDismissRequest = { mostrarMenuPerfil = false },
            title = { Text(if (currentUser != null) "Perfil" else "Iniciar Sesión") },
            text = {
                if (currentUser != null) {
                    Text("Estás conectado con la cuenta:\n${currentUser?.email ?: ""}\n\n¿Deseas cerrar sesión?")
                } else {
                    Text("Iniciá sesión de forma segura usando tu cuenta de Google para respaldar tus datos en la nube.")
                }
            },
            confirmButton = {
                if (currentUser != null) {
                    TextButton(onClick = {
                        authViewModel?.signOut()
                        mostrarMenuPerfil = false
                    }) {
                        Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Button(onClick = {
                        mostrarMenuPerfil = false
                        authViewModel?.signInWithGoogle(context) { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Sesión iniciada correctamente", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Error al iniciar sesión", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Text("Iniciar Sesión con Google")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarMenuPerfil = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
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
                items(clasesDeHoy, key = { it.clase.id }) { claseConAlumnos ->
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
                        }
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
