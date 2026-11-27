package com.jdluu.flexinsight.domain.model

data class TrainingLoadScore(
    val overall: Int,
    val hevyVolumeScore: Int,
    val cardioScore: Int,
    val sleepScore: Int,
    val label: String,
    val detail: String
)

data class DeloadAlert(
    val shouldDeload: Boolean,
    val message: String
)

data class RoutineComparison(
    val routineName: String,
    val regressions: List<String>,
    val improvements: List<String>,
    val summary: String
)
