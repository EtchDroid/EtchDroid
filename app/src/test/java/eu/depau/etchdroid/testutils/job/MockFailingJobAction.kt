package eu.depau.etchdroid.testutils.job

import eu.depau.etchdroid.testutils.worker.MockFailingJobWorker
import eu.depau.etchdroid.utils.StringResBuilder
import org.mockito.Mockito

class MockFailingJobAction(
        name: StringResBuilder,
        progressWeight: Double,
        startAt: Int,
        steps: Int,
        showInGUI: Boolean,
        runAlways: Boolean
) : MockJobAction(name, progressWeight, startAt, steps, showInGUI, runAlways) {

    override val lazyWorker by lazy {
        Mockito.spy(MockFailingJobWorker())
    }
}