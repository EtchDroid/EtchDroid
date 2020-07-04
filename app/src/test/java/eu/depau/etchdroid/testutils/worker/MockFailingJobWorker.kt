package eu.depau.etchdroid.testutils.worker

import eu.depau.etchdroid.testutils.exception.TestJobFailException
import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker

class MockFailingJobWorker : AbstractAutoProgressAsyncWorker(
        0, 1, RateUnit.FURLONGS_PER_FORTNIGHT) {


    /**
     * Make the worker do one step. Returns whether there are more steps to do
     *
     * @return whether more work needs to be done
     */
    override fun runStep(): Boolean {
        throw TestJobFailException("Test job failure")
    }
}