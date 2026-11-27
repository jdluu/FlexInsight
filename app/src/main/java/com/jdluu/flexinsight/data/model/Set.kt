package com.jdluu.flexinsight.data.model

import com.google.gson.annotations.SerializedName

/**
 * API response model for Set
 */
data class SetResponse(
    @SerializedName("index")
    val index: Int,
    @SerializedName("type")
    val type: String?,
    @SerializedName("weight_kg")
    val weightKg: Double?,
    @SerializedName("reps")
    val reps: Int?,
    @SerializedName("rpe")
    val rpe: Double?,
    @SerializedName("distance_meters")
    val distanceMeters: Double?,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    @SerializedName("custom_metric")
    val customMetric: Double?,
    @SerializedName("personal_record")
    val personalRecord: Boolean? = false
)
