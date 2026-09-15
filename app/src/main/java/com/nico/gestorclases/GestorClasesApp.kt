package com.nico.gestorclases

import android.app.Application
import com.nico.gestorclases.data.db.AppDatabase
import com.nico.gestorclases.data.repository.AlumnoRepository
import com.nico.gestorclases.data.repository.ClaseRepository
import com.nico.gestorclases.viewmodel.ViewModelFactory

class GestorClasesApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val alumnoRepository by lazy { AlumnoRepository(database.alumnoDao()) }
    val claseRepository by lazy { ClaseRepository(database.claseDao()) }
    val viewModelFactory by lazy { ViewModelFactory(alumnoRepository, claseRepository) }
}
