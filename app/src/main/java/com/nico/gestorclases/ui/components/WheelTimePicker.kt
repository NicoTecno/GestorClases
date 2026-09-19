package com.nico.gestorclases.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val ITEM_HEIGHT: Dp = 56.dp
private const val VISIBLE_ITEMS = 5
private const val VIRTUAL_MULTIPLIER = 1000

/**
 * Rueda deslizable estilo alarma Samsung.
 *
 * @param items      Lista de strings a mostrar (ej. "00".."59")
 * @param startIndex Índice seleccionado al abrir
 * @param onItemSelected Callback con el índice real cuando el scroll se detiene
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    startIndex: Int = 0,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalHeight = ITEM_HEIGHT * VISIBLE_ITEMS
    val virtualCount = items.size * VIRTUAL_MULTIPLIER

    val startVirtual = run {
        val mid = virtualCount / 2
        mid - (mid % items.size) + startIndex
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = maxOf(0, startVirtual - VISIBLE_ITEMS / 2)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerVirtual by remember {
        derivedStateOf { listState.firstVisibleItemIndex + VISIBLE_ITEMS / 2 }
    }

    LaunchedEffect(centerVirtual) {
        onItemSelected(centerVirtual % items.size)
    }

    Box(
        modifier = modifier.height(totalHeight),
        contentAlignment = Alignment.Center
    ) {
        // Zona de selección resaltada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            items(virtualCount) { virtualIndex ->
                val realIndex = virtualIndex % items.size
                val distFromCenter = abs(virtualIndex - centerVirtual)

                val alpha = when (distFromCenter) {
                    0 -> 1f
                    1 -> 0.55f
                    2 -> 0.25f
                    else -> 0f
                }
                val scale = when (distFromCenter) {
                    0 -> 1f
                    1 -> 0.82f
                    2 -> 0.66f
                    else -> 0.5f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT)
                        .graphicsLayer {
                            this.alpha = alpha
                            this.scaleX = scale
                            this.scaleY = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[realIndex],
                        fontSize = if (distFromCenter == 0) 32.sp else 22.sp,
                        fontWeight = if (distFromCenter == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (distFromCenter == 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Diálogo completo de selección de hora con dos ruedas (HH : mm).
 */
@Composable
fun WheelTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Seleccionar hora",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = hours,
                    startIndex = initialHour,
                    onItemSelected = { selectedHour = it },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = ":",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                WheelPicker(
                    items = minutes,
                    startIndex = initialMinute,
                    onItemSelected = { selectedMinute = it },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar") }
        }
    )
}
