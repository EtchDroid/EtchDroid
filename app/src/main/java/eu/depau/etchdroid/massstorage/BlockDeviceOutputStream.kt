package eu.depau.etchdroid.massstorage

import android.util.Log
import eu.depau.etchdroid.utils.AsyncOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BlockDeviceOutputStream"

private const val BARRIER_TAG = -0xBA881E8L

interface ISeekableStream {
    fun seek(offset: Long): Long {
        return runBlocking { seekAsync(offset) }
    }

    suspend fun seekAsync(offset: Long): Long {
        return seek(offset)
    }
}

class BlockDeviceOutputStream(
    private val blockDev: BlockDeviceDriver,
    private val coroutineScope: CoroutineScope,
    private val bufferBlocks: Long = 512,
    private val queueSize: Int = 4,
) : AsyncOutputStream(), ISeekableStream {

    private var mCurrentBlockOffset: Long = 0

    private val mCurrentOffset: Long
        get() = mCurrentBlockOffset * blockDev.blockSize + mByteBuffer.position()

    private val mSizeBytes: Long
        get() = blockDev.blocks * blockDev.blockSize

    private var mByteBuffer = ByteBuffer.allocate(minOf(blockDev.blockSize * bufferBlocks, mSizeBytes).toInt())

    private val isEOF: Boolean
        get() = mCurrentOffset >= mSizeBytes

    private var mBlockChannel = Channel<Pair<Long, ByteBuffer>>(queueSize)

    private var mFlushRendezvous = Channel<Unit>()

    private val blockDeviceMutex = Mutex()

    private val mIoThreadRunning: AtomicBoolean = AtomicBoolean(false)

    private suspend fun ensureIoThread() {
        if (mIoThreadRunning.get()) return

        val rendezvous = Channel<Unit>()

        coroutineScope.launch(Dispatchers.IO) {
            Thread.currentThread().name = "BlockDeviceOutputStream I/O thread"

            if (!mIoThreadRunning.compareAndSet(false, true)) {
                rendezvous.send(Unit)
                return@launch
            }

            mBlockChannel = Channel(queueSize)
            mFlushRendezvous = Channel()

            // ensureIoThread() will return at this point
            rendezvous.send(Unit)

            try {
                while (true) {
                    val (blockNumber, buffer) = mBlockChannel.receive()

                    if (blockNumber == BARRIER_TAG) {
                        mFlushRendezvous.send(Unit)
                        continue
                    }

                    require(buffer.position() == 0) { "Buffer position must be 0" }
                    if (buffer.limit() == 0) continue

                    // Partial block write
                    if ((buffer.limit() % blockDev.blockSize) != 0) {
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
                    }

                    blockDeviceMutex.withLock {
                        blockDev.write(blockNumber, buffer)
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // Channel closed, stop
            } catch (e: Exception) {
                Log.e(TAG, "Exception in I/O thread", e)
                mBlockChannel.close(e)
                mFlushRendezvous.close(e)
            } finally {
                mIoThreadRunning.set(false)
            }
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
        mBlockChannel.send(Pair(mCurrentBlockOffset, oldBuffer))

        mByteBuffer = newBuffer
        mCurrentBlockOffset += oldBuffer.limit() / blockDev.blockSize
    }


    override suspend fun flushAsync() {
        commit()
        ensureIoThread()
        mBlockChannel.send(Pair(BARRIER_TAG, ByteBuffer.allocate(0)))
        mFlushRendezvous.receive()
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
        flushAsync()
        mBlockChannel.close()
    }

}