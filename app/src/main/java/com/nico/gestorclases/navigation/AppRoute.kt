package com.nico.gestorclases.navigation

import kotlinx.serialization.Serializable

/**
 * Rutas de navegación type-safe (Navigation 2.8+).
 *
 * Cada objeto es una ruta con tipos en tiempo de compilación.
 * El compilador detecta rutas o argumentos mal escritos en vez de fallar en runtime.
 *
 * Uso:
 * ```kotlin
 * navController.navigate(AppRoute.Home)
 * navController.navigate(AppRoute.StudentDetail)
 * ```
 */
sealed interface AppRoute {

    /** Pantalla principal: clases del día de hoy. */
    @Serializable
    data object Home : AppRoute

    /** Pantalla de calendario mensual. */
    @Serializable
    data object Calendar : AppRoute

    /** Pantalla de lista de alumnos. */
    @Serializable
    data object Students : AppRoute

    /** Pantalla de detalle de un alumno seleccionado. */
    @Serializable
    data object StudentDetail : AppRoute
}
