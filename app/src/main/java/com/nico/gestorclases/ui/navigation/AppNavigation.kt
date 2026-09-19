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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nico.gestorclases.GestorClasesApp
import com.nico.gestorclases.navigation.AppRoute
import com.nico.gestorclases.ui.screens.calendar.CalendarScreen
import com.nico.gestorclases.ui.screens.home.HomeScreen
import com.nico.gestorclases.ui.screens.students.StudentDetailScreen
import com.nico.gestorclases.ui.screens.students.StudentsScreen
import com.nico.gestorclases.viewmodel.CalendarViewModel
import com.nico.gestorclases.viewmodel.HomeViewModel
import com.nico.gestorclases.viewmodel.StudentsViewModel
import com.nico.gestorclases.viewmodel.AuthViewModel
import kotlin.reflect.KClass

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
    val authViewModel: AuthViewModel = viewModel(factory = factory)

    // Rutas donde se muestra el BottomBar (ahora basado en las clases de los objetos)
    val bottomBarRoutesClasses = BottomNavItem.items.map { it.route::class }
    val showBottomBar = bottomBarRoutesClasses.any { routeClass ->
        currentDestination?.hasRoute(routeClass) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    BottomNavItem.items.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
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
                val initialIndex = BottomNavItem.items.indexOfFirst { it.route::class.qualifiedName == initialState.destination.route }
                val targetIndex = BottomNavItem.items.indexOfFirst { it.route::class.qualifiedName == targetState.destination.route }
                val direction = if (initialIndex != -1 && targetIndex != -1 && targetIndex < initialIndex) {
                    AnimatedContentTransitionScope.SlideDirection.End
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start
                }
                slideIntoContainer(direction, tween(300))
            },
            exitTransition = {
                val initialIndex = BottomNavItem.items.indexOfFirst { it.route::class.qualifiedName == initialState.destination.route }
                val targetIndex = BottomNavItem.items.indexOfFirst { it.route::class.qualifiedName == targetState.destination.route }
                val direction = if (initialIndex != -1 && targetIndex != -1 && targetIndex < initialIndex) {
                    AnimatedContentTransitionScope.SlideDirection.End
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start
                }
                slideOutOfContainer(direction, tween(300))
            },
            popEnterTransition = {
                val initialIndex = BottomNavItem.items.indexOfFirst { it.route::class.qualifiedName == initialState.destination.route }
                val targetIndex = BottomNavItem.items.indexOfFirst { it.route::class.qualifiedName == targetState.destination.route }
                val direction = if (initialIndex != -1 && targetIndex != -1 && targetIndex < initialIndex) {
                    AnimatedContentTransitionScope.SlideDirection.End
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start
                }
                slideIntoContainer(direction, tween(300))
            },
            popExitTransition = {
                val initialIndex = BottomNavItem.items.indexOfFirst { it.route::class.qualifiedName == initialState.destination.route }
                val targetIndex = BottomNavItem.items.indexOfFirst { it.route::class.qualifiedName == targetState.destination.route }
                val direction = if (initialIndex != -1 && targetIndex != -1 && targetIndex < initialIndex) {
                    AnimatedContentTransitionScope.SlideDirection.End
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start
                }
                slideOutOfContainer(direction, tween(300))
            }
        ) {
            composable<AppRoute.Home> {
                HomeScreen(viewModel = homeViewModel, authViewModel = authViewModel)
            }
            composable<AppRoute.Calendar> {
                CalendarScreen(viewModel = calendarViewModel)
            }
            composable<AppRoute.Students> {
                StudentsScreen(
                    viewModel = studentsViewModel,
                    onNavigateToDetail = { alumno ->
                        studentsViewModel.seleccionarAlumno(alumno)
                        navController.navigate(AppRoute.StudentDetail)
                    }
                )
            }
            composable<AppRoute.StudentDetail> {
                StudentDetailScreen(
                    viewModel = studentsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
