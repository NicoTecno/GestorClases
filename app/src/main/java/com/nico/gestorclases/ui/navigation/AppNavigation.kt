package com.nico.gestorclases.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nico.gestorclases.GestorClasesApp
import com.nico.gestorclases.ui.screens.calendar.CalendarScreen
import com.nico.gestorclases.ui.screens.home.HomeScreen
import com.nico.gestorclases.ui.screens.students.StudentDetailScreen
import com.nico.gestorclases.ui.screens.students.StudentsScreen
import com.nico.gestorclases.viewmodel.CalendarViewModel
import com.nico.gestorclases.viewmodel.HomeViewModel
import com.nico.gestorclases.viewmodel.StudentsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val app = LocalContext.current.applicationContext as GestorClasesApp
    val factory = app.viewModelFactory

    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val calendarViewModel: CalendarViewModel = viewModel(factory = factory)
    val studentsViewModel: StudentsViewModel = viewModel(factory = factory)

    // Rutas donde se muestra el BottomBar
    val bottomBarRoutes = BottomNavItem.items.map { it.route }
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    BottomNavItem.items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Hoy.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(300)
                )
            }
        ) {
            composable(BottomNavItem.Hoy.route) {
                HomeScreen(viewModel = homeViewModel)
            }
            composable(BottomNavItem.Calendario.route) {
                CalendarScreen(viewModel = calendarViewModel)
            }
            composable(BottomNavItem.Alumnos.route) {
                StudentsScreen(
                    viewModel = studentsViewModel,
                    onNavigateToDetail = { alumno ->
                        studentsViewModel.seleccionarAlumno(alumno)
                        navController.navigate("alumno_detalle")
                    }
                )
            }
            composable("alumno_detalle") {
                StudentDetailScreen(
                    viewModel = studentsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
