@file:OptIn(ExperimentalCoroutinesApi::class)

package eu.depau.etchdroid.massstorage

import eu.depau.etchdroid.utils.AsyncOutputStream
import eu.depau.etchdroid.utils.TimedMutex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import java.io.IOException
import java.nio.ByteBuffer

internal data class BlockDeviceChannelOperation(
    val block: Long,
    val data: ByteBuffer,
)

class BlockDeviceChannelOutputStream(
    private val blockDev: BlockDeviceDriver,
    private val coroutineScope: CoroutineScope,
    bufferBlocks: Int = 32,
) : AsyncOutputStream(), ISeekableStream {
    private var currentBlockOffset: Long = 0
    private var byteBuffer = ByteBuffer.allocate(blockDev.blockSize)
    private var channel = Channel<BlockDeviceChannelOperation>(bufferBlocks)
    private val channelOwnershipMutex = TimedMutex()
    private val channelOperationMutex = TimedMutex()
    private var closed = false
    private var _ioThreadException: Exception? = null

    private val currentByteOffset: Long
        get() = currentBlockOffset * blockDev.blockSize + byteBuffer.position()

    private val sizeBytes: Long
        get() = blockDev.blocks * blockDev.blockSize

    private val bytesUntilEOF: Long
        get() = blockDev.blocks * blockDev.blockSize - currentByteOffset

    private val workerStartedLock = TimedMutex(locked = true)

    private val writeJob = coroutineScope.launch(Dispatchers.IO) {
        try {
            while (!closed) {
                channelOwnershipMutex.withLock {
                    println("Channel ownership lock acquired")
//                    DebugProbes.dumpCoroutines()
                    if (workerStartedLock.isLocked)
                        workerStartedLock.unlock()

                    for (op in channel) {
                        blockDev.write(op.block, op.data)
                        println("Wrote block ${op.block}")
                    }
                    // Channel closed; if we're not closed, it got closed just to flush, reopen it
                    channel = Channel(bufferBlocks)
                    println("Reopened channel")
                }
            }
        } catch (e: Exception) {
            _ioThreadException = e
        }
    }

    private suspend inline fun <T> withOperationLock(owner: Any? = null, action: () -> T): T {
        println("Waiting for worker to start")
//        DebugProbes.dumpCoroutines()
        if (workerStartedLock.isLocked)
            workerStartedLock.withLock {}
        return channelOperationMutex.withLock(owner, action)
    }

    private suspend fun enqueueWrite(block: Long, data: ByteBuffer): Unit =
        withOperationLock {
            enqueueWriteNonLocking(block, data)
        }

    private suspend fun enqueueIfNeeded() {
        if (byteBuffer.hasRemaining())
            return

        enqueueWrite(currentBlockOffset, byteBuffer)
        byteBuffer = ByteBuffer.allocate(blockDev.blockSize)
        currentBlockOffset++
    }

    private suspend fun enqueueWriteNonLocking(block: Long, data: ByteBuffer) {
        if (_ioThreadException != null) throw _ioThreadException!!
        channel.send(BlockDeviceChannelOperation(block, data))
    }

    override suspend fun closeAsync(): Unit = withOperationLock {
        closed = true
        flushAsyncNonLocking()
        writeJob.join()
    }

    override suspend fun flushAsync(): Unit = withOperationLock {
        flushAsyncNonLocking()
    }

    private suspend fun flushAsyncNonLocking() {
        channel.close()
        println("Channel closed")
        if (workerStartedLock.isLocked)
            workerStartedLock.withLock {}
        channelOwnershipMutex.withLock {
            println("Channel ownership lock acquired by flush")
//            DebugProbes.dumpCoroutines()

            // Once we reach this point the channel is starved since flushMutex is preventing new
            // writes, and the channel is closed, so all data has been written.

            if (byteBuffer.position() == 0)
                return

            val initialPosition = byteBuffer.position()

            if (byteBuffer.remaining() != 0) {
                // Read one block from the device and merge it with the data to be written
                val block = ByteBuffer.allocate(blockDev.blockSize)
                withContext(Dispatchers.IO) {
                    blockDev.read(currentBlockOffset, block)
                }
                block.apply {
                    position(byteBuffer.position())
                    limit(byteBuffer.capacity())
                    byteBuffer.put(this)
                }
            }

            assert(byteBuffer.remaining() == 0)

            println("Flushing ${byteBuffer.position()} bytes")

            // Write the data
            byteBuffer.flip()
            withContext(Dispatchers.IO) {
                blockDev.write(currentBlockOffset, byteBuffer)
            }

            // Reset the buffer
            byteBuffer.clear()
            if (initialPosition == byteBuffer.capacity()) {
                currentBlockOffset++
            } else {
                byteBuffer.position(initialPosition)
            }
            println("About to release ownership lock")
        }
        println("Flushed")
    }

    override suspend fun seekAsync(offset: Long): Long = withOperationLock {
        flushAsyncNonLocking()

        currentBlockOffset = offset / blockDev.blockSize
        byteBuffer.position((offset % blockDev.blockSize).toInt())
        currentByteOffset

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
                withContext(Dispatchers.IO) {
                    blockDev.read(currentBlockOffset, this@apply)
                }
                clear()
                position(blockByteOffset)
            }
        }

        return actualSkipDistance
    }

    override suspend fun writeAsync(b: Int) {
        if (bytesUntilEOF < 1) {
            flushAsync()
            throw IOException("No space left on device")
        }

        byteBuffer.put(b.toByte())
        enqueueIfNeeded()
    }

    override suspend fun writeAsync(b: ByteArray) {
        writeAsync(b, 0, b.size)
    }

    override suspend fun writeAsync(b: ByteArray, off: Int, len: Int) {
        if (len <= 0 || off > b.size)
            return

        if (bytesUntilEOF < len) {
            flushAsync()
            throw IOException("No space left on device")
        }

        val actualLen = len.coerceAtMost(b.size - off)
        var toWrite = actualLen

        while (toWrite > 0) {
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            val toWriteNow = Math.min(toWrite, byteBuffer.remaining())
            byteBuffer.put(b, off + actualLen - toWrite, toWriteNow)
            toWrite -= toWriteNow
            enqueueIfNeeded()
        }
    }
}
