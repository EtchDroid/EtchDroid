package eu.depau.etchdroid.libaums_wrapper.streams

import eu.depau.etchdroid.libaums_wrapper.kotlinexts.size
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

class BlockDeviceOutputStream(
    private val blockDev: BlockDeviceDriver,
    private val bufferBlocks: Int = 2048
) : OutputStream(), Seekable {

    private val byteBuffer = ByteBuffer.allocate(blockDev.blockSize * bufferBlocks)

    private var currentBlockOffset: Long = 0
    private val currentByteOffset: Long
        get() = currentBlockOffset * blockDev.blockSize + byteBuffer.position()

    private val sizeBytes: Long
        get() = blockDev.size.toLong() * blockDev.blockSize

    private val bytesUntilEOF: Long
        get() = blockDev.size.toLong() * blockDev.blockSize - currentByteOffset

    override fun write(b: Int) {
        if (bytesUntilEOF < 1) {
            flush()
            throw IOException("No space left on device")
        }

        byteBuffer.put(b.toByte())

        if (byteBuffer.remaining() == 0)
            flush()
    }

    override fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        val maxPos = Math.min(off + len, b.size)

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
            byteBuffer.apply {
                position(toWrite)
                limit((fullBlocks + 1) * blockDev.blockSize)
                put(
                    incompleteBlockBuffer.array(),
                    incompleteBlockFullBytes,
                    blockDev.blockSize - incompleteBlockFullBytes
                )
                position(0)
            }
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

        // Ensure the buffer is limited on EOF
        if (blockDev.size - currentBlockOffset < bufferBlocks)
            byteBuffer.limit(
                (blockDev.size - currentBlockOffset).toInt() * blockDev.blockSize
            )

        currentBlockOffset += fullBlocks
    }

    override fun seek(offset: Long): Long {
        // Flush all the unwritten changes to disk
        flush()

        val actualSkipDistance = when {
            currentByteOffset + offset > sizeBytes -> sizeBytes - currentByteOffset
            currentByteOffset + offset < 0 -> -currentByteOffset
            else -> offset
        }

        val newByteOffset = currentByteOffset + actualSkipDistance
        val newBlockOffset = newByteOffset / blockDev.blockSize

        // Jump to the closest block
        currentBlockOffset = newBlockOffset
        byteBuffer.clear()

        // Read the part between the start of the new block and the seeked position
        val blockByteOffset = (newByteOffset - currentBlockOffset * blockDev.blockSize).toInt()
        if (blockByteOffset > 0) {
            byteBuffer.apply {
                position(0)
                limit(blockByteOffset)
                blockDev.read(currentBlockOffset, this)
                clear()
                position(blockByteOffset)
            }
        }

        return actualSkipDistance
    }
}