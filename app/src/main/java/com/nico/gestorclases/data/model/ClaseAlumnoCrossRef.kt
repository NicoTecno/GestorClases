package com.nico.gestorclases.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Tabla intermedia Many-to-Many entre [Clase] y [Alumno].
 * Contiene los datos que son específicos de CADA alumno en UNA clase:
 *  - precioIndividual: puede ser el precio por defecto del alumno u otro acordado.
 *  - estadoPago: permite que cada alumno de un grupo pague en momentos distintos.
 */
@Entity(
    tableName = "clase_alumno_cross_ref",
    primaryKeys = ["claseId", "alumnoId"],
    foreignKeys = [
        ForeignKey(
            entity = Clase::class,
            parentColumns = ["id"],
            childColumns = ["claseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Alumno::class,
            parentColumns = ["id"],
            childColumns = ["alumnoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("claseId"), Index("alumnoId")]
)
data class ClaseAlumnoCrossRef(
    val claseId: String,
    val alumnoId: String,
    val precioIndividual: Double = 0.0,
    val estadoPago: EstadoPago = EstadoPago.PENDIENTE
)
