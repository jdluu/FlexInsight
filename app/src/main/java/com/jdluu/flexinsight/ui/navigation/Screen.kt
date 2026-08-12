package com.jdluu.flexinsight.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object History : Screen("history")
    object AITrainer : Screen("ai_trainer")
    object Planner : Screen("planner")
    object Settings : Screen("settings")
    object PRList : Screen("pr_list")
    object HistoryAnalysis : Screen("history_analysis")
    object WorkoutDetail : Screen("workout_detail/{workoutId}") {
        fun createRoute(workoutId: String) = "workout_detail/$workoutId"
    }
    object Onboarding : Screen("onboarding")
}
