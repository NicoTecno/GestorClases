package com.nico.gestorclases.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.nico.gestorclases.data.db.AppDatabase
import com.nico.gestorclases.data.model.Alumno
import com.nico.gestorclases.data.model.Clase
import com.nico.gestorclases.data.model.ClaseAlumnoCrossRef
import com.nico.gestorclases.data.model.EstadoClase
import com.nico.gestorclases.data.model.EstadoPago
import com.nico.gestorclases.data.model.NivelEducativo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "cloud_sync_prefs"
private const val KEY_LOCAL_LAST_UPDATE = "local_last_update"

/**
 * Gestiona la sincronización bidireccional entre Room (local) y Firestore (nube).
 *
 * ## Estrategia "pull on start, push on change"
 * No usamos WebSockets ni listeners en tiempo real. El flujo es:
 *
 * **Al iniciar sesión o abrir la app (con sesión activa):**
 * 1. Consultamos el campo `lastUpdate` del documento del usuario en Firestore.
 * 2. Lo comparamos con el timestamp de la última modificación local ([KEY_LOCAL_LAST_UPDATE]).
 * 3. Si la nube es más reciente → bajamos todos los datos y reemplazamos Room.
 * 4. Si local es igual o más reciente → no tocamos nada (evita sobreescribir
 *    cambios locales que aún no se subieron por el debounce).
 *
 * **Al hacer cambios locales:**
 * - El upload sync escucha Room con [debounce] de 5 segundos y sube a Firestore.
 * - Actualiza [KEY_LOCAL_LAST_UPDATE] en SharedPreferences para el tracking.
 *
 * ## Caso de uso garantizado
 * - Usás el dispositivo A, hacés cambios, cerrás la app.
 * - Abrís el dispositivo B → pull automático → ves los cambios de A.
 * - Hacés cambios en B → upload → próxima vez que uses A, pull de los cambios de B.
 */
@OptIn(FlowPreview::class)
class CloudSyncManager(
    private val database: AppDatabase,
    private val scope: CoroutineScope,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var syncJob: Job? = null
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Punto de entrada al autenticar un usuario o al iniciar la app con sesión activa.
     * 1. Verifica si la nube tiene datos más nuevos que el local.
     * 2. Si sí → restaura desde la nube.
     * 3. Arranca el ciclo de upload continuo.
     */
    fun startSyncing(user: FirebaseUser) {
        stopSyncing()
        syncJob = scope.launch {
            pullIfCloudIsNewer(user)
            startUploadSync(user)
        }
    }

    /**
     * Compara el timestamp de la nube con el local.
     * Solo hace el pull si la nube tiene datos más recientes.
     * Esto protege contra sobreescribir cambios locales que aún están en el buffer del debounce.
     */
    private suspend fun pullIfCloudIsNewer(user: FirebaseUser) = withContext(Dispatchers.IO) {
        try {
            val userDoc = firestore.collection("users").document(user.uid)
            val snapshot = userDoc.get().await()

            if (!snapshot.exists()) {
                Log.d("CloudSync", "Sin datos en la nube para este usuario.")
                return@withContext
            }

            val cloudLastUpdate = snapshot.getLong("lastUpdate") ?: 0L
            val localLastUpdate = prefs.getLong(KEY_LOCAL_LAST_UPDATE, 0L)

            Log.d("CloudSync", "Cloud lastUpdate: $cloudLastUpdate | Local lastUpdate: $localLastUpdate")

            if (cloudLastUpdate > localLastUpdate) {
                Log.d("CloudSync", "La nube es más nueva → restaurando datos.")
                restoreFromSnapshot(snapshot.get("alumnos"), snapshot.get("clases"), snapshot.get("crossRefs"))
                // Sincronizamos el timestamp local con el de la nube para no re-restaurar innecesariamente
                prefs.edit().putLong(KEY_LOCAL_LAST_UPDATE, cloudLastUpdate).apply()
            } else {
                Log.d("CloudSync", "Local es igual o más nuevo → sin necesidad de restaurar.")
            }
        } catch (e: Exception) {
            Log.e("CloudSync", "Error al verificar sync con la nube", e)
        }
    }

    /**
     * Reemplaza la BD local con los datos de Firestore.
     * El orden de operaciones respeta las foreign keys de Room:
     *   Borrar: CrossRefs → Clases → Alumnos
     *   Insertar: Alumnos → Clases → CrossRefs
     */
    private suspend fun restoreFromSnapshot(rawAlumnos: Any?, rawClases: Any?, rawRefs: Any?) {
        val alumnos = parseAlumnos(rawAlumnos)
        val clases = parseClases(rawClases)
        val crossRefs = parseCrossRefs(rawRefs)

        with(database.claseDao()) {
            deleteAllCrossRefs()
            deleteAllClases()
        }
        database.alumnoDao().deleteAllAlumnos()

        database.alumnoDao().insertAlumnos(alumnos)
        database.claseDao().insertClases(clases)
        database.claseDao().insertCrossRefs(crossRefs)

        Log.d("CloudSync", "Restauración completa: ${alumnos.size} alumnos, ${clases.size} clases.")
    }

    /**
     * Escucha Room con debounce y sube a Firestore cuando hay cambios.
     * Actualiza el timestamp local después de cada upload exitoso.
     */
    private fun CoroutineScope.startUploadSync(user: FirebaseUser) {
        val userDoc = firestore.collection("users").document(user.uid)
        launch {
            combine(
                database.alumnoDao().getAllAlumnos(),
                database.claseDao().getAllClases(),
                database.claseDao().getAllCrossRefs()
            ) { alumnos, clases, refs ->
                Triple(alumnos, clases, refs)
            }
            .debounce(5_000L)
            .collect { (alumnos, clases, refs) ->
                val now = System.currentTimeMillis()

                // Serialización explícita a Maps para evitar problemas con
                // la serialización automática de Room entities por Firestore.
                val alumnosMap = alumnos.map { a ->
                    mapOf(
                        "id" to a.id,
                        "nombre" to a.nombre,
                        "apellido" to a.apellido,
                        "nivelEducativo" to a.nivelEducativo.name,
                        "telefono" to a.telefono,
                        "telefonoTutor" to a.telefonoTutor,
                        "precioPorDefecto" to a.precioPorDefecto,
                        "notas" to a.notas
                    )
                }
                val clasesMap = clases.map { c ->
                    mapOf(
                        "id" to c.id,
                        "fecha" to c.fecha,
                        "horaInicio" to c.horaInicio,
                        "horaFin" to c.horaFin,
                        "estadoClase" to c.estadoClase.name,
                        "notas" to c.notas
                    )
                }
                val crossRefsMap = refs.map { r ->
                    mapOf(
                        "claseId" to r.claseId,
                        "alumnoId" to r.alumnoId,
                        "precioIndividual" to r.precioIndividual,
                        "estadoPago" to r.estadoPago.name
                    )
                }

                val backupData = hashMapOf(
                    "alumnos" to alumnosMap,
                    "clases" to clasesMap,
                    "crossRefs" to crossRefsMap,
                    "lastUpdate" to now
                )
                try {
                    userDoc.set(backupData).await()
                    prefs.edit().putLong(KEY_LOCAL_LAST_UPDATE, now).apply()
                    Log.d("CloudSync", "Upload exitoso: ${alumnos.size} alumnos, ${clases.size} clases, ${refs.size} crossRefs.")
                } catch (e: Exception) {
                    Log.e("CloudSync", "Error al subir a la nube", e)
                }
            }
        }
    }

    fun stopSyncing() {
        syncJob?.cancel()
        syncJob = null
    }

    // ── Parsers de Firestore → modelos de Room ────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun parseAlumnos(raw: Any?): List<Alumno> {
        val list = raw as? List<Map<String, Any?>> ?: return emptyList()
        return list.mapNotNull { map ->
            try {
                val idRaw = map["id"]
                val uuidRaw = map["uuid"]
                val idStr = idRaw?.toString() ?: uuidRaw?.toString() ?: java.util.UUID.randomUUID().toString()
                
                Alumno(
                    id = idStr,
                    nombre = map["nombre"] as? String ?: "",
                    apellido = map["apellido"] as? String ?: "",
                    nivelEducativo = NivelEducativo.valueOf(
                        map["nivelEducativo"] as? String ?: NivelEducativo.PRIMARIA.name
                    ),
                    telefono = map["telefono"] as? String ?: "",
                    telefonoTutor = map["telefonoTutor"] as? String ?: "",
                    precioPorDefecto = (map["precioPorDefecto"] as? Number)?.toDouble() ?: 0.0,
                    notas = map["notas"] as? String ?: ""
                )
            } catch (e: Exception) {
                Log.w("CloudSync", "Error parseando alumno: $map", e)
                null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseClases(raw: Any?): List<Clase> {
        val list = raw as? List<Map<String, Any?>> ?: return emptyList()
        return list.mapNotNull { map ->
            try {
                val idRaw = map["id"]
                val uuidRaw = map["uuid"]
                val idStr = idRaw?.toString() ?: uuidRaw?.toString() ?: java.util.UUID.randomUUID().toString()

                Clase(
                    id = idStr,
                    fecha = (map["fecha"] as? Long) ?: 0L,
                    horaInicio = map["horaInicio"] as? String ?: "00:00",
                    horaFin = map["horaFin"] as? String ?: "00:00",
                    estadoClase = EstadoClase.valueOf(
                        map["estadoClase"] as? String ?: EstadoClase.RESERVADA.name
                    ),
                    notas = map["notas"] as? String ?: ""
                )
            } catch (e: Exception) {
                Log.w("CloudSync", "Error parseando clase: $map", e)
                null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseCrossRefs(raw: Any?): List<ClaseAlumnoCrossRef> {
        val list = raw as? List<Map<String, Any?>> ?: return emptyList()
        return list.mapNotNull { map ->
            try {
                val claseIdRaw = map["claseId"]
                val alumnoIdRaw = map["alumnoId"]
                
                ClaseAlumnoCrossRef(
                    claseId = claseIdRaw as? String ?: claseIdRaw?.toString() ?: "",
                    alumnoId = alumnoIdRaw as? String ?: alumnoIdRaw?.toString() ?: "",
                    precioIndividual = (map["precioIndividual"] as? Number)?.toDouble() ?: 0.0,
                    estadoPago = EstadoPago.valueOf(
                        map["estadoPago"] as? String ?: EstadoPago.PENDIENTE.name
                    )
                )
            } catch (e: Exception) {
                Log.w("CloudSync", "Error parseando crossRef: $map", e)
                null
            }
        }
    }
}
