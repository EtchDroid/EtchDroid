package eu.depau.etchdroid.workers

import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker
import java.io.InputStream
import java.io.OutputStream


open class Input2OutputStreamCopyAsyncWorker(
        private val source: InputStream,
        private val dest: OutputStream,
        chunkSize: Int,
        override val progressUpdateDTO: ProgressUpdateDTO,
        size: Long
) : IMergedAsyncWorkerProgressSender, AbstractAutoProgressAsyncWorker(size) {

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