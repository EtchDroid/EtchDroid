package eu.depau.etchdroid.worker.dto

import eu.depau.etchdroid.worker.enums.OperationType
import eu.depau.etchdroid.worker.enums.StepType
import java.util.*

data class ProgressStartDTO(
        val operationId: Int,
        val operationType: OperationType,
        val inputName: String,
        val outputName: String,
        val steps: List<Pair<String, StepType>>
)