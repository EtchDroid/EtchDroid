package eu.depau.etchdroid.testutils.worker

import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker

open class MockJobWorker(val startAt: Int, val steps: Int) : AbstractAutoProgressAsyncWorker(
        startAt.toLong(), steps.toLong(), RateUnit.FURLONGS_PER_FORTNIGHT) {

    var counter = startAt
    private val sleepTime = (UPDATE_INTERVAL * 2.2 / steps).toLong()

    /**
     * Make the worker do one step. Returns whether there are more steps to do
     *
     * @return whether more work needs to be done
     */
    override fun runStep(): Boolean {
        counter++
        println("    -- Worker counts $counter/$steps")
        progressUpdate(1)
        Thread.sleep(sleepTime)
        return counter < steps
    }
}