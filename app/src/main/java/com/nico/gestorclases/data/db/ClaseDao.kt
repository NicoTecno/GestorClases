package com.nico.gestorclases.data.db

import androidx.room.*
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseConAlumno
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaseDao {

    @Transaction
    @Query("SELECT * FROM clases WHERE fecha = :fecha ORDER BY horaInicio")
    fun getClasesDelDia(fecha: Long): Flow<List<ClaseConAlumno>>

    @Transaction
    @Query(
        """SELECT * FROM clases 
           WHERE fecha >= :inicioMes AND fecha <= :finMes 
           ORDER BY fecha, horaInicio"""
    )
    fun getClasesDelMes(inicioMes: Long, finMes: Long): Flow<List<ClaseConAlumno>>

    @Transaction
    @Query(
        """SELECT * FROM clases 
           WHERE alumnoId = :alumnoId 
           ORDER BY fecha DESC, horaInicio"""
    )
    fun getClasesDeAlumno(alumnoId: Int): Flow<List<ClaseConAlumno>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClase(clase: Clase): Long

    @Update
    suspend fun updateClase(clase: Clase)

    @Delete
    suspend fun deleteClase(clase: Clase)
}
