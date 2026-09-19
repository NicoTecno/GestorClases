package com.nico.gestorclases.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidad central de un evento de clase.
 * Ya NO contiene alumnoId, precioClase ni estadoPago.
 * Esos datos viven en [ClaseAlumnoCrossRef] para soportar clases grupales.
 */
@Entity(tableName = "clases")
data class Clase(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    /** Epoch millis del inicio del día (sin hora) para agrupar por fecha */
    val fecha: Long,
    val horaInicio: String,   // formato "HH:mm"
    val horaFin: String,      // formato "HH:mm"
    val estadoClase: EstadoClase = EstadoClase.RESERVADA,
    val notas: String = ""
)
