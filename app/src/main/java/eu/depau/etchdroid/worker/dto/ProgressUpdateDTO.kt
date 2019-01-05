package eu.depau.etchdroid.worker.dto

import eu.depau.etchdroid.worker.enums.RateUnit

data class ProgressUpdateDTO(
        val operationId: Int,
        val currentStep: Int,
        val operationProgress: Double,
        val stepProgress: Double?,
        val timeRemaining: Long?,
        val currentRate: Double?,
        val rateUnit: RateUnit?
)