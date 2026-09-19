package com.nico.gestorclases

import android.app.Application
import com.nico.gestorclases.data.db.AppDatabase
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.AuthRepository
import com.nico.gestorclases.data.repository.ClaseRepository
import com.nico.gestorclases.data.sync.CloudSyncManager
import com.nico.gestorclases.viewmodel.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GestorClasesApp : Application() {

    /**
     * Scope vinculado al proceso de la aplicación, no a ninguna Activity o ViewModel.
     * - [SupervisorJob]: si un hijo falla, no cancela al resto de los hijos.
     * - [Dispatchers.IO]: contexto adecuado para operaciones de base de datos y red.
     *
     * Este scope se pasa a [CloudSyncManager] para evitar que cree sus propios
     * scopes huérfanos (memory leak).
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val alumnoRepository by lazy { AlumnoRepository(database.alumnoDao()) }
    val claseRepository by lazy { ClaseRepository(database.claseDao()) }
    val authRepository by lazy { AuthRepository() }
    val cloudSyncManager by lazy { CloudSyncManager(database, applicationScope) }
    val viewModelFactory by lazy {
        ViewModelFactory(alumnoRepository, claseRepository, authRepository, cloudSyncManager)
    }
}
