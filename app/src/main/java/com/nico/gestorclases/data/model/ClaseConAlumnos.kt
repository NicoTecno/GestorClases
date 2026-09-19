package com.nico.gestorclases.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Relación Many-to-Many resuelta por Room.
 * Una [Clase] tiene N [Alumno]s a través de [ClaseAlumnoCrossRef].
 *
 * Para obtener el precio/pago de cada alumno usar
 * [ClaseConAlumnos.alumnoConPagos] que incluye la cross-ref directamente.
 */
data class ClaseConAlumnos(
    @Embedded val clase: Clase,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ClaseAlumnoCrossRef::class,
            parentColumn = "claseId",
            entityColumn = "alumnoId"
        )
    )
    val alumnos: List<Alumno>,

    /**
     * Las cross-refs completas, para acceder a precioIndividual y estadoPago
     * de cada alumno sin necesitar una query adicional.
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "claseId"
    )
    val participantes: List<ClaseAlumnoCrossRef>
) {
    /** Precio total de la clase (suma de todos los precios individuales) */
    val precioTotal: Double get() = participantes.sumOf { it.precioIndividual }

    /** True si hay más de un alumno vinculado */
    val esGrupal: Boolean get() = alumnos.size > 1

    /**
     * Devuelve la cross-ref del alumno con [alumnoId] dado.
     * Útil para mostrar precio/pago de un alumno específico en pantallas de detalle.
     */
    fun participanteDe(alumnoId: String): ClaseAlumnoCrossRef? =
        participantes.find { it.alumnoId == alumnoId }
}
