package com.example.flexinsight.ui.utils

object UnitConverter {
    private const val KG_TO_LBS = 2.20462
    
    /**
     * Convert weight from kg to lbs if using imperial
     */
    fun convertWeight(weightKg: Double?, useMetric: Boolean): Double? {
        if (weightKg == null) return null
        return if (useMetric) weightKg else weightKg * KG_TO_LBS
    }
    
    /**
     * Convert volume from kg to lbs if using imperial
     */
    fun convertVolume(volumeKg: Double, useMetric: Boolean): Double {
        return if (useMetric) volumeKg else volumeKg * KG_TO_LBS
    }
    
    /**
     * Get weight unit label
     */
    fun getWeightUnit(useMetric: Boolean): String {
        return if (useMetric) "kg" else "lbs"
    }
    
    /**
     * Format weight value with appropriate unit
     */
    fun formatWeight(weightKg: Double?, useMetric: Boolean): String {
        val weight = convertWeight(weightKg, useMetric) ?: return "-"
        return if (useMetric) {
            String.format("%.0f", weight)
        } else {
            String.format("%.0f", weight)
        }
    }
    
    /**
     * Format volume with appropriate formatting and unit
     */
    fun formatVolume(volumeKg: Double, useMetric: Boolean): String {
        val volume = convertVolume(volumeKg, useMetric)
        if (volume <= 0) return "0"
        if (volume >= 1000) {
            val thousands = volume / 1000.0
            return if (thousands % 1.0 == 0.0) {
                "${thousands.toInt()}k"
            } else {
                String.format("%.1fk", thousands)
            }
        }
        return if (volume % 1.0 == 0.0) {
            volume.toInt().toString()
        } else {
            String.format("%.1f", volume)
        }
    }
    
    /**
     * Format volume with commas
     */
    fun formatVolumeWithCommas(volumeKg: Double, useMetric: Boolean): String {
        val volume = convertVolume(volumeKg, useMetric)
        val volumeInt = volume.toInt()
        return volumeInt.toString().reversed().chunked(3).joinToString(",").reversed()
    }
}
