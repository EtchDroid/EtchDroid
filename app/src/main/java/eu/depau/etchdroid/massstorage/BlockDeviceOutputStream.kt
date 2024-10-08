package eu.depau.etchdroid.massstorage

import android.util.Log
import eu.depau.etchdroid.utils.AsyncOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BlockDeviceOutputStream"

private const val BARRIER_TAG = -0xBA881E8L

private const val TRACE_IO = false

@JvmStatic
private fun BlockDeviceOutputStream.traceIo(msg: String) {
    if (TRACE_IO) println("OSTREAM: ${Thread.currentThread().name} time ${System.nanoTime()} pos $mCurrentOffset $msg")
}


class BlockDeviceOutputStream(
    private val blockDev: BlockDeviceDriver,
    private val coroutineScope: CoroutineScope,
    private val bufferBlocks: Long = 512,
    queueSize: Int = 4,
) : AsyncOutputStream(), ISeekableStream {

    private var mCurrentBlockOffset: Long = 0

    internal val mCurrentOffset: Long
        get() = mCurrentBlockOffset * blockDev.blockSize + mByteBuffer.position()

    private val mSizeBytes: Long
        get() = blockDev.blocks * blockDev.blockSize

    private var mByteBuffer = ByteBuffer.allocate(minOf(blockDev.blockSize * bufferBlocks, mSizeBytes).toInt())

    private val isEOF: Boolean
        get() = mCurrentOffset >= mSizeBytes

    private val mChannelMutex = Mutex()

    private val mBlockChannel = Channel<Pair<Long, ByteBuffer>>(queueSize)

    private val mFlushRendezvous = Channel<Unit>()

    private val blockDeviceMutex = Mutex()

    private val mIoThreadRunning: AtomicBoolean = AtomicBoolean(false)

    private val closed = AtomicBoolean(false)

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private suspend fun ensureIoThread() {
        if (mIoThreadRunning.get()) return

        val rendezvous = Channel<Unit>()

        mChannelMutex.withLock {
            mBlockChannel.let {
                if (it.isClosedForReceive) {
                    // Try to fetch the exception
                    while (!it.isEmpty) it.receive()
                    throw IllegalStateException("Channel closed for receive")
                } else if (it.isClosedForSend) {
                    it.send(Pair(BARRIER_TAG, ByteBuffer.allocate(0)))
                    throw IllegalStateException("Channel closed for send")
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            Thread.currentThread().name = "BlockDeviceOutputStream I/O thread"

            if (!mIoThreadRunning.compareAndSet(false, true)) {
                rendezvous.send(Unit)
                return@launch
            }

            // ensureIoThread() will return at this point
            rendezvous.send(Unit)

            traceIo("start")

            val blockChannel = mChannelMutex.withLock { mBlockChannel }
            val flushRendezvous = mChannelMutex.withLock { mFlushRendezvous }

            try {
                while (true) {
                    val (blockNumber, buffer) = blockChannel.receive()

                    if (blockNumber == BARRIER_TAG) {
                        traceIo("pop BARRIER")
                        flushRendezvous.send(Unit)
                        continue
                    }
                    traceIo("block $blockNumber pop size ${buffer.limit()}")

                    require(buffer.position() == 0) { "Buffer position must be 0" }
                    if (buffer.limit() == 0) continue

                    // Partial block write
                    if ((buffer.limit() % blockDev.blockSize) != 0) {
                        traceIo("block $blockNumber partial write")
                        val lastBlockNumber = blockNumber + buffer.limit() / blockDev.blockSize
                        val lastBlockOffset = buffer.limit() - buffer.limit() % blockDev.blockSize
                        val lastBlockBlankOffset = buffer.limit() % blockDev.blockSize

                        val partialData = ByteBuffer.allocate(blockDev.blockSize).apply {
                            blockDeviceMutex.withLock {
                                blockDev.read(lastBlockNumber, this)
                            }
                        }
                        buffer.apply {
                            position(lastBlockOffset + lastBlockBlankOffset)
                            limit(lastBlockOffset + blockDev.blockSize)
                            put(
                                partialData.array(), lastBlockBlankOffset, blockDev.blockSize - lastBlockBlankOffset
                            )
                            position(0)
                        }
                        traceIo("block $blockNumber partial write done")
                    }

                    traceIo("block $blockNumber write")
                    blockDeviceMutex.withLock {
                        blockDev.write(blockNumber, buffer)
                    }
                    traceIo("block $blockNumber write done")
                }
            } catch (e: ClosedReceiveChannelException) {
                // Channel closed, stop
            } catch (e: Exception) {
                closed.set(true)

                Log.e(TAG, "Exception in I/O thread", e)
                // Try to empty the channel to unblock any send() calls already in progress
                while (!blockChannel.isEmpty) blockChannel.tryReceive()

                blockChannel.close(e)
                flushRendezvous.close(e)
            }

            traceIo("end")

            // Only set the flag to false if no exception was thrown; exceptions are not recoverable
            mIoThreadRunning.set(false)
        }

        rendezvous.receive()
    }

    private suspend fun commit() {
        if (mByteBuffer.position() == 0) return

        val oldBuffer = mByteBuffer.apply { flip() }

        val newOffset = mCurrentOffset + oldBuffer.limit()
        val capacity = minOf(
            blockDev.blockSize.toLong() * bufferBlocks, mSizeBytes - newOffset
        )
        val newBuffer = ByteBuffer.allocate(capacity.toInt())

        if (oldBuffer.limit() % blockDev.blockSize != 0) {
            // Copy the unfinished block to the new buffer, without changing the position in the old buffer
            oldBuffer.apply {
                position(limit() - limit() % blockDev.blockSize)
                newBuffer.put(this)
                position(0)
            }
        }

        ensureIoThread()
        val channel = mChannelMutex.withLock { mBlockChannel }
        traceIo("block $mCurrentBlockOffset push size ${oldBuffer.limit()}")
        channel.send(Pair(mCurrentBlockOffset, oldBuffer))
        traceIo("block $mCurrentBlockOffset push done")
        mByteBuffer = newBuffer
        mCurrentBlockOffset += oldBuffer.limit() / blockDev.blockSize
    }


    override suspend fun flushAsync() {
        commit()
        ensureIoThread()
        val channel = mChannelMutex.withLock { mBlockChannel }
        channel.send(Pair(BARRIER_TAG, ByteBuffer.allocate(0)))
        val rendezvous = mChannelMutex.withLock { mFlushRendezvous }
        rendezvous.receive()
    }

    override suspend fun seekAsync(offset: Long): Long {
        if (offset == 0L) return 0L

        val actualSkipDistance = when {
            mCurrentOffset + offset > mSizeBytes -> mSizeBytes - mCurrentOffset
            mCurrentOffset + offset < 0 -> -mCurrentOffset
            else -> offset
        }

        val newByteOffset = mCurrentOffset + actualSkipDistance
        val newBlockOffset = newByteOffset / blockDev.blockSize

        // Check if the new offset is within the written range of the current buffer
        if (newBlockOffset in mCurrentBlockOffset until mCurrentBlockOffset + mByteBuffer.position() / blockDev.blockSize) {
            val bufferByteOffset = (newBlockOffset - mCurrentBlockOffset) * blockDev.blockSize + newByteOffset
            mByteBuffer.position(bufferByteOffset.toInt())
            return actualSkipDistance
        }

        // Flush all the unwritten changes to disk
        flushAsync()

        // Jump to the closest block
        mCurrentBlockOffset = newBlockOffset
        mByteBuffer.clear()

        if (offset % blockDev.blockSize != 0L) {
            // Read the part between the start of the new block and the sought position
            val blockByteOffset = offset % blockDev.blockSize
            mByteBuffer.apply {
                position(0)
                limit(blockDev.blockSize)
                blockDeviceMutex.withLock {
                    withContext(Dispatchers.IO) {
                        blockDev.read(newBlockOffset, this@apply)
                    }
                }
                clear()
                position(blockByteOffset.toInt())
            }
        }

        return actualSkipDistance
    }

    override suspend fun writeAsync(b: Int) {
        if (isEOF) throw IOException("No space left on device")

        mByteBuffer.put((b and 0xFF).toByte())
        if (!mByteBuffer.hasRemaining()) commit()
    }

    override suspend fun writeAsync(b: ByteArray, off: Int, len: Int) {
        if (len <= 0 || off > b.size) return

        if (isEOF) throw IOException("No space left on device")

        val actualLen = len.coerceAtMost(b.size - off)
        var toWrite = actualLen

        while (toWrite > 0) {
            if (isEOF) throw IOException("No space left on device")

            val toWriteNow = toWrite.coerceAtMost(mByteBuffer.remaining())
            mByteBuffer.put(b, off + actualLen - toWrite, toWriteNow)
            toWrite -= toWriteNow

            if (toWrite > 0 && !mByteBuffer.hasRemaining()) commit()
        }

        if (!mByteBuffer.hasRemaining()) commit()
    }

    override suspend fun writeAsync(b: ByteArray) {
        writeAsync(b, 0, b.size)
    }

    override suspend fun closeAsync() {
        if (closed.getAndSet(true)) return
        flushAsync()
        val channel = mChannelMutex.withLock { mBlockChannel }
        channel.close()
    }
}
