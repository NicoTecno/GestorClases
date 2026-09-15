package com.nico.gestorclases.data.repository

import com.nico.gestorclases.data.db.ClaseDao
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseConAlumno
import kotlinx.coroutines.flow.Flow

class ClaseRepository(private val claseDao: ClaseDao) {

    fun getClasesDelDia(fecha: Long): Flow<List<ClaseConAlumno>> =
        claseDao.getClasesDelDia(fecha)

    fun getClasesDelMes(inicioMes: Long, finMes: Long): Flow<List<ClaseConAlumno>> =
        claseDao.getClasesDelMes(inicioMes, finMes)

    fun getClasesDeAlumno(alumnoId: Int): Flow<List<ClaseConAlumno>> =
        claseDao.getClasesDeAlumno(alumnoId)

    suspend fun insertarClase(clase: Clase): Long = claseDao.insertClase(clase)

    suspend fun actualizarClase(clase: Clase) = claseDao.updateClase(clase)

    suspend fun eliminarClase(clase: Clase) = claseDao.deleteClase(clase)
}
