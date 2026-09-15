package com.nico.gestorclases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nico.gestorclases.data.model.ClaseConAlumno
import com.nico.gestorclases.data.model.EstadoClase
import com.nico.gestorclases.ui.theme.*
import com.nico.gestorclases.utils.DateUtils.formatPrice

@Composable
fun ClaseCard(
    claseConAlumno: ClaseConAlumno,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clase = claseConAlumno.clase
    val alumno = claseConAlumno.alumno

    val borderColor = when (clase.estadoClase) {
        EstadoClase.RESERVADA -> StatusReservada
        EstadoClase.DADA -> StatusDada
        EstadoClase.CANCELADA_CON_TIEMPO -> StatusCanceladaTiempo
        EstadoClase.CANCELADA_MISMO_DIA -> StatusCanceladaMismoDia
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra de color lateral
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
                // Fila superior: nombre + precio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alumno.nombreCompleto,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatPrice(clase.precioClase),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Fila: hora + nivel
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
                    NivelBadge(label = alumno.nivelEducativo.displayName)
                }

                Spacer(Modifier.height(8.dp))

                // Fila: badges de estado
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EstadoClaseBadge(estado = clase.estadoClase)
                    EstadoPagoBadge(estado = clase.estadoPago)
                }

                // Notas (si hay)
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
