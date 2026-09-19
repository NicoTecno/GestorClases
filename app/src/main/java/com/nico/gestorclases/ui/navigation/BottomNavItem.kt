package com.nico.gestorclases.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.ui.graphics.vector.ImageVector
import com.nico.gestorclases.navigation.AppRoute
import kotlin.reflect.KClass

sealed class BottomNavItem(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Calendario : BottomNavItem(
        route = AppRoute.Calendar,
        label = "Calendario",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object Hoy : BottomNavItem(
        route = AppRoute.Home,
        label = "Hoy",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Alumnos : BottomNavItem(
        route = AppRoute.Students,
        label = "Alumnos",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    )

    companion object {
        val items = listOf(Calendario, Hoy, Alumnos)
        
        // Función de ayuda para ver si el route actual coincide con la clase del AppRoute
        fun isRouteSelected(currentRouteClassName: String?, itemRouteClass: KClass<out AppRoute>): Boolean {
            return currentRouteClassName == itemRouteClass.qualifiedName
        }
    }
}
