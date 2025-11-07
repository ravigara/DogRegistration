package com.example.dogregistration.camera

/**
 * Simple brightness/variance analysis for captured images.
 */
data class QualityMetrics(val brightness: Double, val variance: Double, val isGood: Boolean)

class ImageQualityAnalyzer {
    fun analyzeLuma(bytes: ByteArray): QualityMetrics {
        val mean = bytes.map { it.toInt() and 0xFF }.average()
        val variance = bytes.map { (it.toInt() and 0xFF) - mean }
            .map { it * it }.average()
        val isGood = mean in 30.0..220.0 && variance > 120
        return QualityMetrics(mean, variance, isGood)
    }
}