package eu.depau.etchdroid.workers

import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker
import java.io.InputStream
import java.io.OutputStream

class StreamCloseWorker(
        private val sharedData: MutableMap<SharedDataType, Any?>,
        private val destKey: SharedDataType
// Progress is actually never sent so I'm using bogus values here because it still needs to
// implement IWorkerProgressSender
) : AbstractAutoProgressAsyncWorker(0, 0, RateUnit.BYTES_PER_SECOND) {

    override fun runStep(): Boolean {
        when (val stream = sharedData[destKey]) {
            is InputStream  -> stream.close()
            is OutputStream -> stream.close()
        }
        sharedData.remove(destKey)

        return false
    }

}