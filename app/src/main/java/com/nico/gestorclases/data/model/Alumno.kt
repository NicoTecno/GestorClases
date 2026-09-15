package com.nico.gestorclases.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alumnos")
data class Alumno(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val nivelEducativo: NivelEducativo,
    val telefono: String = "",
    val telefonoTutor: String = "",
    val precioPorDefecto: Double = 0.0,
    val notas: String = ""
) {
    val nombreCompleto: String get() = "$nombre $apellido"
    val iniciales: String get() = "${nombre.firstOrNull() ?: ""}${apellido.firstOrNull() ?: ""}".uppercase()
}
