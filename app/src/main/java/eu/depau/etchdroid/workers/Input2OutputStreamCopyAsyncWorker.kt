package eu.depau.etchdroid.workers

import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker
import java.io.InputStream
import java.io.OutputStream


open class Input2OutputStreamCopyAsyncWorker(
        private val source: InputStream,
        private val dest: OutputStream,
        private val seek: Long,
        chunkSize: Int = 512 * 32 * 64, // 1 MB
        size: Long
) :
        IMergedAsyncWorkerProgressSender,
        AbstractAutoProgressAsyncWorker(
                seek, size, RateUnit.BYTES_PER_SECOND
        ) {

    private var seeked = false

    private val buffer = ByteArray(chunkSize)

    override fun runStep(): Boolean {
        if (!seeked && seek > 0) {
            TODO("seek output")
            //source.skip(seek)
        }

        val readBytes = source.read(buffer)

        if (readBytes < 0)
            return false

        dest.write(buffer, 0, readBytes)
        progressUpdate(readBytes.toLong())

        return true
    }
}