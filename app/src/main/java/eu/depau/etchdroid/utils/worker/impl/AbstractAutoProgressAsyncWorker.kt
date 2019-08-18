package eu.depau.etchdroid.utils.worker.impl

import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO
import eu.depau.etchdroid.utils.worker.enums.RateUnit


abstract class AbstractAutoProgressAsyncWorker(
        private val alreadyDone: Long,
        private val totalToDo: Long,
        private val rateUnit: RateUnit
) : AbstractAsyncWorker() {

    private var doneAccumulator = alreadyDone
    private var doneSinceLastUpdate = 0L
    private var lastUpdateTime = 0L

    fun progressUpdate(stepDone: Long) {
        val currentTime = System.currentTimeMillis()

        doneSinceLastUpdate += stepDone
        doneAccumulator += stepDone

        if (currentTime > lastUpdateTime + UPDATE_INTERVAL) {
            val progress = doneAccumulator.toDouble() / totalToDo
            val speedUnitPerMillis = doneSinceLastUpdate.toDouble() / (currentTime - lastUpdateTime)

            val dto = ProgressUpdateDTO(
                    progress = progress,
                    rate = speedUnitPerMillis * 1000,
                    rateUnit = rateUnit
            )

            notifyWorkerProgress(dto)

            doneSinceLastUpdate = 0
            lastUpdateTime = currentTime
        }
    }

    companion object {
        const val UPDATE_INTERVAL = 500
    }
}