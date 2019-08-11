package eu.depau.etchdroid.utils.worker.impl

import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO

const val UPDATE_INTERVAL = 500

abstract class AbstractAutoProgressAsyncWorker(private val totalToDo: Long) : AbstractAsyncWorker() {
    abstract val progressUpdateDTO: ProgressUpdateDTO

    private var doneAccumulator = 0L
    private var doneSinceLastUpdate = 0L
    private var lastUpdateTime = 0L

    fun progressUpdate(lastDone: Long) {
        val currentTime = System.currentTimeMillis()

        if (lastUpdateTime == 0L)
            lastUpdateTime = currentTime

        doneSinceLastUpdate += lastDone
        doneAccumulator += lastDone

        if (currentTime > lastUpdateTime + UPDATE_INTERVAL) {
            val progress = doneAccumulator.toDouble() / totalToDo
            val speedUnitPerMillis = doneSinceLastUpdate.toDouble() / (currentTime - lastUpdateTime)
            val timeRemainingMillis: Long = ((totalToDo - doneAccumulator) / speedUnitPerMillis).toLong()

            val dto = progressUpdateDTO.copy(
                    jobProgress = progress,
                    stepProgress = progress,
                    timeRemaining = timeRemainingMillis,
                    currentRate = speedUnitPerMillis * 1000
            )

            notifyProgress(dto)

            doneSinceLastUpdate = 0
            lastUpdateTime = currentTime
        }
    }
}