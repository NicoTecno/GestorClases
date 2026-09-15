package com.nico.gestorclases.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clases",
    foreignKeys = [
        ForeignKey(
            entity = Alumno::class,
            parentColumns = ["id"],
            childColumns = ["alumnoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("alumnoId")]
)
data class Clase(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val alumnoId: Int,
    /** Epoch millis del inicio del día (sin hora) para agrupar por fecha */
    val fecha: Long,
    val horaInicio: String,   // formato "HH:mm"
    val horaFin: String,      // formato "HH:mm"
    val precioClase: Double,
    val estadoClase: EstadoClase = EstadoClase.RESERVADA,
    val estadoPago: EstadoPago = EstadoPago.PENDIENTE,
    val notas: String = ""
)
