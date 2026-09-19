package com.nico.gestorclases.data.db

import androidx.room.*
import com.nico.gestorclases.data.model.Alumno
import kotlinx.coroutines.flow.Flow

@Dao
interface AlumnoDao {

    @Query("SELECT * FROM alumnos ORDER BY apellido, nombre")
    fun getAllAlumnos(): Flow<List<Alumno>>

    @Query(
        """SELECT * FROM alumnos 
           WHERE nombre LIKE '%' || :query || '%' 
           OR apellido LIKE '%' || :query || '%'
           ORDER BY apellido, nombre"""
    )
    fun searchAlumnos(query: String): Flow<List<Alumno>>

    @Query("SELECT * FROM alumnos WHERE id = :id")
    suspend fun getAlumnoById(id: String): Alumno?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlumno(alumno: Alumno)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlumnos(alumnos: List<Alumno>)

    @Query("DELETE FROM alumnos")
    suspend fun deleteAllAlumnos()

    @Update
    suspend fun updateAlumno(alumno: Alumno)

    @Delete
    suspend fun deleteAlumno(alumno: Alumno)
}
