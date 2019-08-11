package eu.depau.etchdroid.testutils.worker

import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker
import eu.depau.etchdroid.utils.worker.impl.UPDATE_INTERVAL

open class MockJobWorker(override val progressUpdateDTO: ProgressUpdateDTO) : AbstractAutoProgressAsyncWorker(10) {
    var counter = 0
    val steps = 10
    val sleepTime = (UPDATE_INTERVAL * 2.2 / steps).toLong()

    /**
     * Make the worker do one step. Returns whether there are more steps to do
     *
     * @return whether more work needs to be done
     */
    override fun runStep(): Boolean {
        counter++
        Thread.sleep(sleepTime)
        return counter < steps
    }
}