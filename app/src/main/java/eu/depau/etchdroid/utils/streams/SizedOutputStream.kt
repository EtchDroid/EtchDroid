package eu.depau.etchdroid.utils.streams

import java.io.OutputStream

/**
 * This class simply wraps an existing OutputStream adding some provided size field.
 */
class SizedOutputStream(
        override val size: Long,
        private val outputStream: OutputStream
) : AbstractSizedOutputStream() {
    override fun write(b: Int) {
        outputStream.write(b)
    }

    override fun write(b: ByteArray?) {
        outputStream.write(b)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        outputStream.write(b, off, len)
    }

    override fun flush() {
        outputStream.flush()
    }

    override fun close() {
        outputStream.close()
    }
}