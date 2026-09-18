package com.nico.gestorclases.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.nico.gestorclases.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CloudSyncManager(
    private val database: AppDatabase
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var syncJob: Job? = null

    /**
     * Comienza a escuchar los cambios en la base de datos local y los sube a Firestore.
     * Solo funciona si hay un usuario logueado.
     */
    fun startSyncing(user: FirebaseUser) {
        stopSyncing() // Detener cualquier sincronización previa
        val userDoc = firestore.collection("users").document(user.uid)

        // Lanzamos una corrutina que vivirá mientras la app esté activa o hasta que se cierre sesión
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            // Combinar los tres flujos de la base de datos local
            combine(
                database.alumnoDao().getAllAlumnos(),
                database.claseDao().getAllClases(),
                database.claseDao().getAllCrossRefs()
            ) { alumnos, clases, refs ->
                
                // Creamos un snapshot de la base de datos completa
                val backupData = hashMapOf(
                    "alumnos" to alumnos,
                    "clases" to clases,
                    "crossRefs" to refs,
                    "lastUpdate" to System.currentTimeMillis()
                )
                
                try {
                    // Subir a Firestore
                    userDoc.set(backupData).await()
                    Log.d("CloudSync", "Copia de seguridad subida exitosamente.")
                } catch (e: Exception) {
                    Log.e("CloudSync", "Error al subir a la nube", e)
                }

            }.collect { }
        }
    }

    /**
     * Detiene la sincronización (usualmente al cerrar sesión).
     */
    fun stopSyncing() {
        syncJob?.cancel()
        syncJob = null
    }
}
