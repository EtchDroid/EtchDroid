package eu.depau.etchdroid.utils.blockdevice

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

class BlockDeviceOutputStream(
        private val blockDev: BlockDeviceDriver,
        bufferBlocks: Int = 2048
) : OutputStream() {

    private val byteBuffer = ByteBuffer.allocate(blockDev.blockSize * bufferBlocks)

    private var currentBlockOffset: Long = 0
    private val currentByteOffset: Long
        get() = currentBlockOffset * blockDev.blockSize + byteBuffer.position()

    private val sizeBytes: Long
        get() = blockDev.size.toLong() * blockDev.blockSize

    private val bytesUntilEOF: Long
        get() = blockDev.size.toLong() * blockDev.size - currentByteOffset

    override fun write(b: Int) {
        if (bytesUntilEOF < 1)
            throw IOException("No space left on device")

        byteBuffer.put(b.toByte())

        if (byteBuffer.remaining() == 0)
            flush()
    }

    override fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        val maxPos = (off + len) % (b.size + 1)

        if (len <= 0 || off > b.size)
            return

        for (i in off until maxPos)
            write(b[i].toInt())
    }

    override fun flush() {
        byteBuffer.flip()

        val toWrite = byteBuffer.limit()
        val incompleteBlockFullBytes = toWrite % blockDev.blockSize
        val fullBlocks = (toWrite - incompleteBlockFullBytes) / blockDev.blockSize

        // Check if we're trying to flush while the last written block isn't full
        if (incompleteBlockFullBytes > 0) {
            val incompleteBlockBuffer = ByteBuffer.allocate(blockDev.blockSize)

            // Load last block from device
            blockDev.read(currentBlockOffset + fullBlocks, incompleteBlockBuffer)

            // Add it to the incomplete block
            byteBuffer.limit(fullBlocks * blockDev.blockSize)
            byteBuffer.put(
                    incompleteBlockBuffer.array(),
                    toWrite, blockDev.blockSize - incompleteBlockFullBytes
            )
            byteBuffer.position(0)
        }

        // Flush to device
        blockDev.write(currentBlockOffset, byteBuffer)

        // Copy the incomplete block at the beginning, then push back the position
        byteBuffer.apply {
            position(fullBlocks * blockDev.blockSize)
            limit(toWrite)
            compact()
            clear()
            position(incompleteBlockFullBytes)
        }
        currentBlockOffset += fullBlocks;
    }
}