package com.nico.gestorclases.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.nico.gestorclases.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Escucha los cambios en la base de datos local y los sincroniza con Firestore.
 *
 * Diseño:
 * - Recibe un [CoroutineScope] externo ([applicationScope] de [GestorClasesApp]) para no
 *   crear scopes huérfanos que provoquen memory leaks.
 * - Usa [debounce] para agrupar ráfagas de cambios rápidos (ej: editar varios alumnos
 *   seguidos) y evitar escrituras innecesarias a Firestore.
 * - Los efectos secundarios (subida a Firestore) van en [collect], no en el transform de
 *   [combine], que es la separación correcta de responsabilidades.
 */
@OptIn(FlowPreview::class)
class CloudSyncManager(
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var syncJob: Job? = null

    /**
     * Comienza a escuchar los cambios en la base de datos local y los sube a Firestore.
     * Cancela cualquier sincronización previa antes de iniciar una nueva.
     */
    fun startSyncing(user: FirebaseUser) {
        stopSyncing()
        val userDoc = firestore.collection("users").document(user.uid)

        syncJob = scope.launch {
            combine(
                database.alumnoDao().getAllAlumnos(),
                database.claseDao().getAllClases(),
                database.claseDao().getAllCrossRefs()
            ) { alumnos, clases, refs ->
                // Solo transforma los datos; el efecto secundario (subida) va en collect
                Triple(alumnos, clases, refs)
            }
            // Agrupa cambios que lleguen dentro de una ventana de 5 segundos.
            // Evita llamadas repetidas a Firestore en operaciones por lotes.
            .debounce(5_000L)
            .collect { (alumnos, clases, refs) ->
                val backupData = hashMapOf(
                    "alumnos" to alumnos,
                    "clases" to clases,
                    "crossRefs" to refs,
                    "lastUpdate" to System.currentTimeMillis()
                )
                try {
                    userDoc.set(backupData).await()
                    Log.d("CloudSync", "Copia de seguridad subida exitosamente.")
                } catch (e: Exception) {
                    Log.e("CloudSync", "Error al subir a la nube", e)
                }
            }
        }
    }

    /**
     * Detiene la sincronización activa (usualmente al cerrar sesión).
     * El [scope] externo sigue vivo; solo se cancela el [Job] de este ciclo.
     */
    fun stopSyncing() {
        syncJob?.cancel()
        syncJob = null
    }
}
