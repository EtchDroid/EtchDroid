package eu.depau.etchdroid.testutils.worker

import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker

open class MockJobWorker(startAt: Int, val steps: Int) : AbstractAutoProgressAsyncWorker(10, RateUnit.FURLONGS_PER_FORTNIGHT) {
    var counter = startAt
    private val sleepTime = (UPDATE_INTERVAL * 2.2 / steps).toLong()

    /**
     * Make the worker do one step. Returns whether there are more steps to do
     *
     * @return whether more work needs to be done
     */
    override fun runStep(): Boolean {
        counter++
        println("    -> Worker counts $counter/$steps")
        Thread.sleep(sleepTime)
        return counter < steps
    }
}