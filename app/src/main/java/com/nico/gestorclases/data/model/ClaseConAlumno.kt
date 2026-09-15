package com.nico.gestorclases.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ClaseConAlumno(
    @Embedded val clase: Clase,
    @Relation(
        parentColumn = "alumnoId",
        entityColumn = "id"
    )
    val alumno: Alumno
)
