package eu.depau.etchdroid.workers

import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker
import java.io.InputStream
import java.io.OutputStream


open class Input2OutputStreamCopyAsyncWorker(
        private val source: InputStream,
        private val dest: OutputStream,
        chunkSize: Int,
        size: Long
) : IMergedAsyncWorkerProgressSender, AbstractAutoProgressAsyncWorker(size, RateUnit.BYTES_PER_SECOND) {

    private val buffer = ByteArray(chunkSize)

    override fun runStep(): Boolean {
        val readBytes = source.read(buffer)

        if (readBytes < 0)
            return false

        dest.write(buffer, 0, readBytes)
        progressUpdate(readBytes.toLong())

        return true
    }
}