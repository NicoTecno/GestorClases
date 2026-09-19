package com.nico.gestorclases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.ClaseConAlumnos
import com.nico.gestorclases.data.model.EstadoClase
import com.nico.gestorclases.data.model.EstadoPago
import com.nico.gestorclases.ui.theme.*
import com.nico.gestorclases.utils.DateUtils.formatPrice

/**
 * Tarjeta de clase adaptada al nuevo modelo Many-to-Many.
 *
 * @param claseConAlumnos La clase con su lista de alumnos y participantes.
 * @param onEdit Callback al tocar el botón de lápiz.
 * @param onMarkDada Marcar el evento completo como Dada (si no es null y la clase es RESERVADA).
 * @param onMarkPagado Marcar el pago de un participante específico. Recibe la crossRef a actualizar.
 * @param alumnoFiltradoId Si se especifica, solo muestra el precio/pago de ese alumno (para pantalla de detalle).
 */
@Composable
fun ClaseCard(
    claseConAlumnos: ClaseConAlumnos,
    onEdit: () -> Unit,
    onMarkDada: (() -> Unit)? = null,
    onMarkPagado: ((ClaseAlumnoCrossRef) -> Unit)? = null,
    alumnoFiltradoId: String? = null,
    modifier: Modifier = Modifier
) {
    val clase = claseConAlumnos.clase
    val alumnos = claseConAlumnos.alumnos
    val participantes = claseConAlumnos.participantes

    // Si hay un alumno filtrado (pantalla de detalle), mostramos solo sus datos
    val participantesFiltrados = if (alumnoFiltradoId != null) {
        participantes.filter { it.alumnoId == alumnoFiltradoId }
    } else {
        participantes
    }

    val borderColor = when (clase.estadoClase) {
        EstadoClase.RESERVADA -> StatusReservada
        EstadoClase.DADA -> StatusDada
        EstadoClase.CANCELADA_CON_TIEMPO -> StatusCanceladaTiempo
        EstadoClase.CANCELADA_MISMO_DIA -> StatusCanceladaMismoDia
    }

    val todosPagado = participantesFiltrados.isNotEmpty() &&
            participantesFiltrados.all { it.estadoPago == EstadoPago.PAGADA }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra de color lateral según estado
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ── Fila superior: nombre(s) + ícono grupal + precio total ────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (claseConAlumnos.esGrupal) {
                            Icon(
                                Icons.Filled.Group,
                                contentDescription = "Clase grupal",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        val nombresTexto = if (alumnoFiltradoId != null) {
                            alumnos.find { it.id == alumnoFiltradoId }?.nombreCompleto ?: ""
                        } else {
                            alumnos.joinToString(", ") { it.nombreCompleto }
                        }
                        Text(
                            text = nombresTexto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                    }
                    // Precio: si hay filtro mostramos precio individual, si no precio total
                    val precioMostrar = if (alumnoFiltradoId != null) {
                        participantesFiltrados.firstOrNull()?.precioIndividual ?: 0.0
                    } else {
                        claseConAlumnos.precioTotal
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatPrice(precioMostrar),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ── Fila: hora + nivel (del primero o del alumno filtrado) ─────
                val alumnoParaNivel = if (alumnoFiltradoId != null) {
                    alumnos.find { it.id == alumnoFiltradoId }
                } else {
                    alumnos.firstOrNull()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${clase.horaInicio} – ${clase.horaFin}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    alumnoParaNivel?.let {
                        NivelBadge(label = it.nivelEducativo.displayName)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Badges de estado + acciones rápidas ───────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EstadoClaseBadge(estado = clase.estadoClase)
                        // Badge de pago: Pagada si todos pagaron, Pendiente si alguno falta
                        if (todosPagado) {
                            EstadoPagoBadge(estado = EstadoPago.PAGADA)
                        } else {
                            EstadoPagoBadge(estado = EstadoPago.PENDIENTE)
                        }
                    }

                    Row {
                        // Botón "Marcar Dada" (para el evento completo)
                        if (clase.estadoClase == EstadoClase.RESERVADA && onMarkDada != null) {
                            IconButton(onClick = onMarkDada, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Marcar Dada",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Botón "Marcar Pagado" — por alumno si hay filtro, o el primero si es individual
                        if (!todosPagado && onMarkPagado != null) {
                            val refAPagar = participantesFiltrados
                                .firstOrNull { it.estadoPago == EstadoPago.PENDIENTE }
                            if (refAPagar != null) {
                                IconButton(
                                    onClick = { onMarkPagado(refAPagar) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AttachMoney,
                                        contentDescription = "Marcar Pagada",
                                        tint = StatusPagada
                                    )
                                }
                            }
                        }
                        // Botón editar
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Editar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Avatares de alumnos adicionales (si es grupal) ───────────
                if (claseConAlumnos.esGrupal && alumnoFiltradoId == null && alumnos.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        alumnos.take(4).forEach { alumno ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = alumno.iniciales,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (alumnos.size > 4) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${alumnos.size - 4}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // ── Notas ─────────────────────────────────────────────────────
                if (clase.notas.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = clase.notas,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
