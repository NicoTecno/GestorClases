package com.nico.gestorclases.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.data.model.NivelEducativo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlumnoDialog(
    alumno: Alumno? = null,
    onDismiss: () -> Unit,
    onConfirm: (Alumno) -> Unit
) {
    val esEdicion = alumno != null

    var nombre by remember { mutableStateOf(alumno?.nombre ?: "") }
    var apellido by remember { mutableStateOf(alumno?.apellido ?: "") }
    var nivelEducativo by remember { mutableStateOf(alumno?.nivelEducativo ?: NivelEducativo.SECUNDARIA) }
    var telefono by remember { mutableStateOf(alumno?.telefono ?: "") }
    var telefonoTutor by remember { mutableStateOf(alumno?.telefonoTutor ?: "") }
    var precio by remember { mutableStateOf(alumno?.precioPorDefecto?.toString() ?: "") }
    var notas by remember { mutableStateOf(alumno?.notas ?: "") }

    var mostrarNivelDropdown by remember { mutableStateOf(false) }

    val formularioValido = nombre.isNotBlank() && apellido.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (esEdicion) "Editar alumno" else "Nuevo alumno") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nombre y apellido
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = apellido,
                        onValueChange = { apellido = it },
                        label = { Text("Apellido *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Nivel educativo
                ExposedDropdownMenuBox(
                    expanded = mostrarNivelDropdown,
                    onExpandedChange = { mostrarNivelDropdown = it }
                ) {
                    OutlinedTextField(
                        value = nivelEducativo.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nivel educativo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostrarNivelDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = mostrarNivelDropdown,
                        onDismissRequest = { mostrarNivelDropdown = false }
                    ) {
                        NivelEducativo.entries.forEach { nivel ->
                            DropdownMenuItem(
                                text = { Text(nivel.displayName) },
                                onClick = {
                                    nivelEducativo = nivel
                                    mostrarNivelDropdown = false
                                }
                            )
                        }
                    }
                }

                // Precio por defecto
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio por defecto ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Se auto-completa al crear una clase") }
                )

                // Teléfonos
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono del alumno") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = telefonoTutor,
                    onValueChange = { telefonoTutor = it },
                    label = { Text("Teléfono del tutor (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                // Notas
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Text(
                    "* Campos obligatorios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val precioDouble = precio.toDoubleOrNull() ?: 0.0
                    val nuevoAlumno = Alumno(
                        id = alumno?.id ?: java.util.UUID.randomUUID().toString(),
                        nombre = nombre.trim(),
                        apellido = apellido.trim(),
                        nivelEducativo = nivelEducativo,
                        telefono = telefono.trim(),
                        telefonoTutor = telefonoTutor.trim(),
                        precioPorDefecto = precioDouble,
                        notas = notas.trim()
                    )
                    onConfirm(nuevoAlumno)
                },
                enabled = formularioValido
            ) {
                Text(if (esEdicion) "Guardar" else "Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
