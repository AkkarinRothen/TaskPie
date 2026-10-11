package com.taskpie.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.taskpie.app.ui.screens.*
import com.taskpie.app.ui.viewmodel.TaskPieViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object ActiveSession : Screen("active_session")
    data object CreateActivity : Screen("edit_activity")
    data object EditActivity : Screen("edit_activity?activityId={activityId}") {
        fun createRoute(activityId: String) = "edit_activity?activityId=$activityId"
    }
    data object CreateRoutine : Screen("edit_routine")
    data object EditRoutine : Screen("edit_routine?routineId={routineId}") {
        fun createRoute(routineId: String) = "edit_routine?routineId=$routineId"
    }
    data object CreateProject : Screen("edit_project")
    data object EditProject : Screen("edit_project?projectId={projectId}") {
        fun createRoute(projectId: String) = "edit_project?projectId=$projectId"
    }
}

@Composable
fun TaskPieNavHost(
    navController: NavHostController,
    viewModel: TaskPieViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToActiveSession = { navController.navigate(Screen.ActiveSession.route) },
                onNavigateToCreateActivity = { navController.navigate(Screen.CreateActivity.route) },
                onNavigateToEditActivity = { id -> navController.navigate(Screen.EditActivity.createRoute(id)) },
                onNavigateToCreateRoutine = { navController.navigate(Screen.CreateRoutine.route) },
                onNavigateToEditRoutine = { id -> navController.navigate(Screen.EditRoutine.createRoute(id)) },
                onNavigateToCreateProject = { navController.navigate(Screen.CreateProject.route) },
                onNavigateToEditProject = { id -> navController.navigate(Screen.EditProject.createRoute(id)) }
            )
        }

        composable(Screen.ActiveSession.route) {
            ActiveSessionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditActivity.route,
            arguments = listOf(
                navArgument("activityId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")
            EditActivityScreen(
                viewModel = viewModel,
                activityId = activityId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditRoutine.route,
            arguments = listOf(
                navArgument("routineId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId")
            EditRoutineScreen(
                viewModel = viewModel,
                routineId = routineId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditProject.route,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")
            EditProjectScreen(
                viewModel = viewModel,
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToActiveSession = { navController.navigate(Screen.ActiveSession.route) }
            )
        }
    }
}
