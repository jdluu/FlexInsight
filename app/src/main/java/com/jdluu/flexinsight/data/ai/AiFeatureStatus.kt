package com.jdluu.flexinsight.data.ai

/**
 * High-level readiness of on-device Gemini Nano via ML Kit / AICore.
 */
sealed interface AiFeatureStatus {
    data object Ready : AiFeatureStatus
    data object Downloadable : AiFeatureStatus
    data object Downloading : AiFeatureStatus
    data object Unavailable : AiFeatureStatus
}
