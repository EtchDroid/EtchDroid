package eu.depau.etchdroid.utils.blockdevice

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import java.io.InputStream
import java.nio.ByteBuffer

class BlockDeviceInputStream(
        private val blockDev: BlockDeviceDriver,
        private val prefetchBlocks: Int = 2048
) : InputStream() {
    private var neverFetched = true

    private val byteBuffer = ByteBuffer.allocate(blockDev.blockSize * prefetchBlocks)

    private var currentBlockOffset: Long = 0
    private val currentByteOffset: Long
        get() = currentBlockOffset * blockDev.blockSize + byteBuffer.position()

    private var markedBlock: Long = 0
    private var markedBufferPosition: Int = 0

    private val sizeBytes: Long
        get() = blockDev.size.toLong() * blockDev.blockSize

    private fun isNextByteAfterEOF(): Boolean {
        if (byteBuffer.hasRemaining())
            return false
        return currentBlockOffset + 1 >= blockDev.size
    }

    private fun fetch() {
        byteBuffer.clear()

        if (blockDev.size - currentBlockOffset < prefetchBlocks)
            byteBuffer.limit(
                    (blockDev.size - currentBlockOffset).toInt() * blockDev.blockSize
            )

        blockDev.read(currentBlockOffset, byteBuffer)
        byteBuffer.flip()
    }

    private fun fetchNextIfNeeded() {
        if (neverFetched) {
            fetch()
            neverFetched = false
            return
        }
        if (byteBuffer.hasRemaining())
            return
        currentBlockOffset++
        fetch()
    }

    override fun read(): Int {
        if (isNextByteAfterEOF())
            return -1
        fetchNextIfNeeded()
        return byteBuffer.get().toInt() and 0xFF
    }

    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (isNextByteAfterEOF())
            return -1

        val maxPos = Math.min(off + len, b.size)

        if (len <= 0 || off > b.size)
            return 0

        var bytesRead = 0

        for (i in off until maxPos) {
            val readByte = read()

            if (readByte == -1)
                break

            b[i] = readByte.toByte()
            bytesRead++
        }

        return bytesRead
    }

    override fun skip(n: Long): Long {
        val actualSkipDistance = when {
            currentByteOffset + n > sizeBytes -> sizeBytes - currentByteOffset
            currentByteOffset + n < 0         -> -currentByteOffset
            else                              -> n
        }

        val newByteOffset = currentByteOffset + actualSkipDistance
        val newBlockOffset = newByteOffset / blockDev.blockSize

        if (newBlockOffset != currentBlockOffset) {
            currentBlockOffset = newBlockOffset
            fetch()
        }

        byteBuffer.position((newByteOffset - currentBlockOffset * blockDev.blockSize).toInt())

        return actualSkipDistance
    }

    override fun available(): Int {
        return byteBuffer.remaining()
    }

    override fun mark(readlimit: Int) {
        markedBlock = currentBlockOffset
        markedBufferPosition = byteBuffer.position()
    }

    override fun markSupported(): Boolean {
        return true
    }

    override fun reset() {
        currentBlockOffset = markedBlock
        fetch()
        byteBuffer.position(markedBufferPosition)
    }
}