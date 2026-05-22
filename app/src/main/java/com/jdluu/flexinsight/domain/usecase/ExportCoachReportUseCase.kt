package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.repository.FlexRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportCoachReportUseCase @Inject constructor(
    private val flexRepository: FlexRepository,
    private val buildAiContextUseCase: BuildAiContextUseCase,
    private val aiClient: FlexAIClient,
    private val calculateTrainingLoadUseCase: CalculateTrainingLoadUseCase
) {
    suspend operator fun invoke(): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("# FlexInsight Weekly Coach Report")
        sb.appendLine("Generated: ${dateFmt.format(Date())}")
        sb.appendLine()

        val stats = runCatching { flexRepository.calculateStats() }.getOrNull()
        stats?.let {
            sb.appendLine("## Summary")
            sb.appendLine("- Total workouts: ${it.totalWorkouts}")
            sb.appendLine("- Total volume: ${"%.0f".format(it.totalVolume)} kg")
            sb.appendLine("- Current streak: ${it.currentStreak} days")
            sb.appendLine()
        }

        val load = calculateTrainingLoadUseCase()
        sb.appendLine("## Training load")
        sb.appendLine("- Overall: ${load.overall}/100 (${load.label})")
        sb.appendLine("- ${load.detail}")
        sb.appendLine()

        val prs = runCatching { flexRepository.getPRsWithDetails(10) }.getOrDefault(emptyList())
        if (prs.isNotEmpty()) {
            sb.appendLine("## Recent PRs")
            prs.forEach { pr ->
                sb.appendLine("- ${pr.exerciseName}: ${pr.weight} kg")
            }
            sb.appendLine()
        }

        val context = buildAiContextUseCase().text
        if (aiClient.isAvailable()) {
            val prompt = "$context\n\nWrite a 3-paragraph weekly coaching summary based on this data. Be specific."
            when (val ai = aiClient.generateResponse(prompt)) {
                is Result.Success -> {
                    sb.appendLine("## AI Coach Notes")
                    sb.appendLine(ai.data)
                }
                is Result.Error -> sb.appendLine("## AI Coach Notes\nUnavailable (${ai.error.message})")
            }
        }

        return sb.toString()
    }
}
