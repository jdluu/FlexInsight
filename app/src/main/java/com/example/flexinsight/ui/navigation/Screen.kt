package com.example.flexinsight.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object History : Screen("history")
    object AITrainer : Screen("ai_trainer")
    object Planner : Screen("planner")
    object Recovery : Screen("recovery")
    object Settings : Screen("settings")
    object WorkoutDetail : Screen("workout_detail/{workoutId}") {
        fun createRoute(workoutId: String) = "workout_detail/$workoutId"
    }
}

