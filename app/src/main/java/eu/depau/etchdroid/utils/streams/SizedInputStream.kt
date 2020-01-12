package eu.depau.etchdroid.utils.streams

import java.io.InputStream

/**
 * This class simply wraps an existing InputStream adding some provided size field.
 */
class SizedInputStream(
        override val size: Long,
        private val inputStream: InputStream
) : AbstractSizedInputStream() {

    override fun skip(n: Long): Long {
        return inputStream.skip(n)
    }

    override fun available(): Int {
        return inputStream.available()
    }

    override fun reset() {
        inputStream.reset()
    }

    override fun close() {
        inputStream.close()
    }

    override fun mark(readlimit: Int) {
        inputStream.mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return inputStream.markSupported()
    }

    override fun read(): Int {
        return inputStream.read()
    }

    override fun read(b: ByteArray?): Int {
        return inputStream.read(b)
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return inputStream.read(b, off, len)
    }
}