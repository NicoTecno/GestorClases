package com.nico.gestorclases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nico.gestorclases.data.model.EstadoClase
import com.nico.gestorclases.data.model.EstadoPago
import com.nico.gestorclases.ui.theme.*

@Composable
fun EstadoClaseBadge(
    estado: EstadoClase,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (estado) {
        EstadoClase.RESERVADA -> StatusReservada to "Reservada"
        EstadoClase.DADA -> StatusDada to "Dada"
        EstadoClase.CANCELADA_CON_TIEMPO -> StatusCanceladaTiempo to "Cancelada"
        EstadoClase.CANCELADA_MISMO_DIA -> StatusCanceladaMismoDia to "Canc. mismo día"
    }
    StatusBadge(color = color, label = label, modifier = modifier)
}

@Composable
fun EstadoPagoBadge(
    estado: EstadoPago,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (estado) {
        EstadoPago.PAGADA -> StatusPagada to "Pagada"
        EstadoPago.PENDIENTE -> StatusPendiente to "Pendiente"
    }
    StatusBadge(color = color, label = label, modifier = modifier)
}

@Composable
private fun StatusBadge(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun NivelBadge(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
