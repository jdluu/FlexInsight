package com.jdluu.flexinsight.domain.ai

import com.jdluu.flexinsight.data.model.CreateRoutineBody
import com.jdluu.flexinsight.data.model.CreateRoutineExercise
import com.jdluu.flexinsight.data.model.CreateRoutineRequest
import com.jdluu.flexinsight.data.model.CreateRoutineSet

data class RoutineParseResult(
    val request: CreateRoutineRequest,
    val matchedExercises: List<String>,
    val unmatchedExercises: List<String>,
    val usedPlaceholder: Boolean
)

/**
 * Parses AI-generated workout plan text into a Hevy routine request.
 * Falls back to a title-only routine with one placeholder set when parsing fails.
 */
object AiRoutineParser {

    private val exerciseLineRegex = Regex(
        """(?i)^\s*(?:\d+[\.\)]\s*)?([A-Za-z][A-Za-z0-9\s\-'/]+?)\s*(?:[-:]\s*)?(\d+)\s*[x×]\s*(\d+(?:\.\d+)?)\s*(?:kg|lbs)?""",
        RegexOption.MULTILINE
    )

    fun parse(planText: String, title: String, templateIdResolver: (String) -> String?): RoutineParseResult {
        val exercises = mutableListOf<CreateRoutineExercise>()
        val matched = mutableListOf<String>()
        val unmatched = mutableListOf<String>()

        for (line in planText.lines()) {
            val match = exerciseLineRegex.find(line.trim()) ?: continue
            val name = match.groupValues[1].trim()
            val sets = match.groupValues[2].toIntOrNull() ?: 1
            val reps = match.groupValues[3].toIntOrNull() ?: 8
            val templateId = templateIdResolver(name)
            if (templateId == null) {
                unmatched.add(name)
                continue
            }

            matched.add(name)
            exercises.add(
                CreateRoutineExercise(
                    exerciseTemplateId = templateId,
                    sets = (1..sets).map {
                        CreateRoutineSet(type = "normal", weightKg = null, reps = reps)
                    }
                )
            )
        }

        val usedPlaceholder = exercises.isEmpty()
        val bodyExercises = if (usedPlaceholder) {
            listOf(
                CreateRoutineExercise(
                    exerciseTemplateId = "0",
                    sets = listOf(CreateRoutineSet(type = "normal", reps = 1))
                )
            )
        } else {
            exercises
        }

        return RoutineParseResult(
            request = CreateRoutineRequest(
                routine = CreateRoutineBody(
                    title = title,
                    exercises = bodyExercises
                )
            ),
            matchedExercises = matched,
            unmatchedExercises = unmatched.distinct(),
            usedPlaceholder = usedPlaceholder
        )
    }
}
