package com.jdluu.flexinsight.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Recovery : Screen("recovery")
    object History : Screen("history")
    object AITrainer : Screen("ai_trainer")
    object Planner : Screen("planner")
    object Settings : Screen("settings")
    object PRList : Screen("pr_list")
    object HistoryAnalysis : Screen("history_analysis")
    object WorkoutDetail : Screen("workout_detail/{workoutId}") {
        fun createRoute(workoutId: String) = "workout_detail/$workoutId"
    }
    object PlateRack : Screen("plate_rack?weight={weight}&unit={unit}") {
        const val BASE_ROUTE = "plate_rack"
        fun createRoute(weight: Double? = null, unit: String? = null): String {
            val params = buildList {
                if (weight != null && weight > 0.0) add("weight=$weight")
                if (!unit.isNullOrBlank()) add("unit=$unit")
            }
            return if (params.isEmpty()) BASE_ROUTE else "$BASE_ROUTE?${params.joinToString("&")}"
        }
    }
    object Onboarding : Screen("onboarding")
}
