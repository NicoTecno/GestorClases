package com.nico.gestorclases.data.repository

import com.nico.gestorclases.data.db.AlumnoDao
import com.nico.gestorclases.data.model.Alumno
import kotlinx.coroutines.flow.Flow

class AlumnoRepository(private val alumnoDao: AlumnoDao) {

    val todosLosAlumnos: Flow<List<Alumno>> = alumnoDao.getAllAlumnos()

    fun buscarAlumnos(query: String): Flow<List<Alumno>> = alumnoDao.searchAlumnos(query)

    suspend fun getAlumnoById(id: Int): Alumno? = alumnoDao.getAlumnoById(id)

    suspend fun insertarAlumno(alumno: Alumno): Long = alumnoDao.insertAlumno(alumno)

    suspend fun actualizarAlumno(alumno: Alumno) = alumnoDao.updateAlumno(alumno)

    suspend fun eliminarAlumno(alumno: Alumno) = alumnoDao.deleteAlumno(alumno)
}
