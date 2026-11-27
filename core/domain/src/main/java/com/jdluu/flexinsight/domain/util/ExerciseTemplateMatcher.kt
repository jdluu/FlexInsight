package com.jdluu.flexinsight.domain.util

/**
 * Resolves AI-parsed exercise names to Hevy template IDs using a name lookup map (id → title).
 */
object ExerciseTemplateMatcher {

    fun resolveTemplateId(exerciseName: String, idToName: Map<String, String>): String? {
        val normalized = exerciseName.trim()
        if (normalized.isEmpty() || idToName.isEmpty()) return null

        idToName.entries.firstOrNull { (_, templateName) ->
            templateName.equals(normalized, ignoreCase = true)
        }?.key?.let { return it }

        idToName.entries.firstOrNull { (_, templateName) ->
            normalized.contains(templateName, ignoreCase = true) ||
                templateName.contains(normalized, ignoreCase = true)
        }?.key?.let { return it }

        return null
    }
}
