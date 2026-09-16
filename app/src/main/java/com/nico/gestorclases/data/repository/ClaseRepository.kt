package com.nico.gestorclases.data.repository

import com.nico.gestorclases.data.db.ClaseDao
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.ClaseConAlumnos
import com.nico.gestorclases.utils.DateUtils.haySolapamiento
import kotlinx.coroutines.flow.Flow

class ClaseRepository(private val claseDao: ClaseDao) {

    fun getClasesDelDia(fecha: Long): Flow<List<ClaseConAlumnos>> =
        claseDao.getClasesDelDia(fecha)

    fun getClasesDelMes(inicioMes: Long, finMes: Long): Flow<List<ClaseConAlumnos>> =
        claseDao.getClasesDelMes(inicioMes, finMes)

    fun getClasesDeAlumno(alumnoId: Int): Flow<List<ClaseConAlumnos>> =
        claseDao.getClasesDeAlumno(alumnoId)

    // ───────────────────────── Inserción ─────────────────────────

    /**
     * Crea una clase nueva (individual o grupal) en una transacción atómica.
     */
    suspend fun insertarClaseConAlumnos(
        clase: Clase,
        crossRefs: List<ClaseAlumnoCrossRef>
    ): Long = claseDao.insertClaseConAlumnos(clase, crossRefs)

    // ───────────────────────── Actualización ─────────────────────

    /**
     * Actualiza solo los campos de la clase (fecha, horas, estado, notas).
     * NO toca los participantes.
     */
    suspend fun actualizarClase(clase: Clase) = claseDao.updateClase(clase)

    /**
     * Actualiza la clase Y reemplaza todos sus participantes.
     * Usar al editar una clase grupal completa.
     */
    suspend fun actualizarClaseConAlumnos(
        clase: Clase,
        crossRefs: List<ClaseAlumnoCrossRef>
    ) = claseDao.updateClaseConAlumnos(clase, crossRefs)

    /**
     * Actualiza solo la cross-ref de un alumno en una clase
     * (ej: marcar un pago individual dentro de un grupo).
     */
    suspend fun actualizarParticipante(crossRef: ClaseAlumnoCrossRef) =
        claseDao.updateCrossRef(crossRef)

    // ───────────────────────── Eliminación ───────────────────────

    suspend fun eliminarClase(clase: Clase) = claseDao.deleteClase(clase)

    // ───────────────────── Validación de Horario ─────────────────

    /**
     * Verifica si existe solapamiento entre el horario propuesto y cualquier clase
     * ya registrada en el mismo día.
     *
     * @param fecha Millis del día a verificar.
     * @param horaInicio Hora de inicio propuesta en formato "HH:mm".
     * @param horaFin Hora de fin propuesta en formato "HH:mm".
     * @param claseIdIgnorar ID de la clase a ignorar (para no bloquearse a sí misma en edición).
     *                       Pasar 0 al crear una clase nueva.
     * @return true si hay solapamiento (NO se puede guardar), false si el horario está libre.
     */
    suspend fun haySolapamientoDeHorario(
        fecha: Long,
        horaInicio: String,
        horaFin: String,
        claseIdIgnorar: Int = 0
    ): Boolean {
        val clasesDelDia = claseDao.getClasesDelDiaSuspend(fecha)
        return clasesDelDia
            .filter { it.id != claseIdIgnorar }
            .any { existente ->
                haySolapamiento(horaInicio, horaFin, existente.horaInicio, existente.horaFin)
            }
    }
}
