package com.example.flexinsight.data.model

import androidx.compose.runtime.Stable

/**
 * Core muscle groups for categorization and heatmap visualization.
 */
@Stable
enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    LEGS("Legs"),
    CORE("Core"),
    CARDIO("Cardio");

    companion object {
        fun fromString(name: String?): MuscleGroup? {
            if (name == null) return null
            val normalized = name.lowercase()
            return when {
                normalized.contains("chest") -> CHEST
                normalized.contains("back") -> BACK
                normalized.contains("shoulder") || normalized.contains("delt") -> SHOULDERS
                normalized.contains("arm") || normalized.contains("bicep") || normalized.contains("tricep") -> ARMS
                normalized.contains("leg") || normalized.contains("quad") || normalized.contains("hamstring") || normalized.contains("calf") || normalized.contains("glute") -> LEGS
                normalized.contains("core") || normalized.contains("ab") -> CORE
                normalized.contains("cardio") -> CARDIO
                else -> null
            }
        }
    }
}
