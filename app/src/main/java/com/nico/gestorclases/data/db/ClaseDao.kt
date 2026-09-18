package com.nico.gestorclases.data.db

import androidx.room.*
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.ClaseConAlumnos
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaseDao {

    // ───────────────────────────── Consultas de Clase ─────────────────────────────

    @Transaction
    @Query("SELECT * FROM clases WHERE fecha = :fecha ORDER BY horaInicio")
    fun getClasesDelDia(fecha: Long): Flow<List<ClaseConAlumnos>>

    @Transaction
    @Query(
        """SELECT * FROM clases 
           WHERE fecha >= :inicioMes AND fecha <= :finMes 
           ORDER BY fecha, horaInicio"""
    )
    fun getClasesDelMes(inicioMes: Long, finMes: Long): Flow<List<ClaseConAlumnos>>

    /**
     * Devuelve todas las clases en las que participa un alumno específico.
     * Usa la tabla intermedia para filtrar.
     */
    @Transaction
    @Query(
        """SELECT c.* FROM clases c
           INNER JOIN clase_alumno_cross_ref cr ON c.id = cr.claseId
           WHERE cr.alumnoId = :alumnoId
           ORDER BY c.fecha DESC, c.horaInicio"""
    )
    fun getClasesDeAlumno(alumnoId: Int): Flow<List<ClaseConAlumnos>>

    /**
     * Devuelve las clases de un día específico (para validación anti-solapamiento).
     * Devuelve una lista simple de [Clase] (sin relaciones) para ser rápida.
     */
    @Query("SELECT * FROM clases WHERE fecha = :fecha")
    suspend fun getClasesDelDiaSuspend(fecha: Long): List<Clase>

    @Query("SELECT * FROM clases")
    fun getAllClases(): Flow<List<Clase>>

    @Query("SELECT * FROM clase_alumno_cross_ref")
    fun getAllCrossRefs(): Flow<List<ClaseAlumnoCrossRef>>

    // ───────────────────────────── Inserts y Updates ────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClase(clase: Clase): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: ClaseAlumnoCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<ClaseAlumnoCrossRef>)

    /**
     * Inserta la clase y todas sus cross-refs en una sola transacción atómica.
     * Retorna el ID de la clase creada.
     */
    @Transaction
    suspend fun insertClaseConAlumnos(
        clase: Clase,
        crossRefs: List<ClaseAlumnoCrossRef>
    ): Long {
        val claseId = insertClase(clase)
        val refsConId = crossRefs.map { it.copy(claseId = claseId.toInt()) }
        insertCrossRefs(refsConId)
        return claseId
    }

    @Update
    suspend fun updateClase(clase: Clase)

    @Update
    suspend fun updateCrossRef(crossRef: ClaseAlumnoCrossRef)

    // ───────────────────────────── Deletes ────────────────────────────────────

    @Delete
    suspend fun deleteClase(clase: Clase)

    /** Elimina todos los participantes de una clase (útil antes de re-insertar al editar). */
    @Query("DELETE FROM clase_alumno_cross_ref WHERE claseId = :claseId")
    suspend fun deleteCrossRefsDeClase(claseId: Int)

    /**
     * Actualiza la clase y reemplaza todos sus participantes en una transacción.
     * Se usa al editar una clase existente.
     */
    @Transaction
    suspend fun updateClaseConAlumnos(
        clase: Clase,
        crossRefs: List<ClaseAlumnoCrossRef>
    ) {
        updateClase(clase)
        deleteCrossRefsDeClase(clase.id)
        val refsConId = crossRefs.map { it.copy(claseId = clase.id) }
        insertCrossRefs(refsConId)
    }
}
