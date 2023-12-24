package eu.depau.etchdroid.massstorage

import android.util.Log
import eu.depau.etchdroid.utils.AsyncInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "BlockDeviceInputStream"

class BlockDeviceInputStream(
    private val blockDev: BlockDeviceDriver,
    private val coroutineScope: CoroutineScope,
    private val bufferBlocks: Int = 512,
    private val prefetchBuffers: Int = 4,
) : AsyncInputStream(), ISeekableStream {
    private lateinit var mReadBuffer: ByteBuffer

    private var mCurrentBlockOffset: Long = 0

    private val mCurrentByteOffset: Int
        get() = if (::mReadBuffer.isInitialized) mReadBuffer.position() else 0

    private val mCurrentOffset: Long
        get() = mCurrentBlockOffset * blockDev.blockSize + mCurrentByteOffset

    private val sizeBytes: Long
        get() = blockDev.blocks * blockDev.blockSize

    private val isEOF: Boolean
        get() = mCurrentOffset >= sizeBytes

    private lateinit var mBlockChannel: Channel<Pair<Long, ByteBuffer>>

    private var markedPosition: Long? = null

    private val mReadNextBlockNumber: AtomicLong = AtomicLong(0)

    private val mIoThreadRunning: AtomicBoolean = AtomicBoolean(false)

    private suspend fun waitAndGetBuffer(
        wantedBlockNumber: Long,
        timeoutMillis: Long = 5000L,
    ): Pair<Long, ByteBuffer> = withTimeout(timeoutMillis) {
        if (!::mBlockChannel.isInitialized) throw IllegalStateException("Channel not initialized")

        while (true) {
            val (blockNumber, buffer) = mBlockChannel.receive()
            if (wantedBlockNumber in blockNumber until blockNumber + bufferBlocks) return@withTimeout Pair(
                blockNumber, buffer
            )
        }

        @Suppress("UNREACHABLE_CODE") throw IllegalStateException("Unreachable")
    }

    private fun tryGetBufferNonBlocking(wantedBlockNumber: Long): Pair<Long, ByteBuffer>? {
        if (!::mBlockChannel.isInitialized) return null
        while (true) {
            val result = mBlockChannel.tryReceive()
            if (result.isSuccess) {
                val (blockNumber, buffer) = result.getOrThrow()
                if (wantedBlockNumber in blockNumber until blockNumber + bufferBlocks) return Pair(
                    blockNumber, buffer
                )
            } else {
                return null
            }
        }
    }

    private suspend fun ensureIoThread() {
        if (mIoThreadRunning.get()) return

        val rendezvous = Channel<Unit>()

        coroutineScope.launch(Dispatchers.IO) {
            Thread.currentThread().name = "BlockDeviceInputStream I/O thread"

            try {
                if (!mIoThreadRunning.compareAndSet(false, true)) {
                    rendezvous.send(Unit)
                    return@launch
                }

                mBlockChannel = Channel(prefetchBuffers)
                rendezvous.send(Unit)

                while (true) {
                    val blockNumber = mReadNextBlockNumber.getAndIncrement()
                    if (blockNumber !in 0 until blockDev.blocks) break

                    try {
                        val buffer = ByteBuffer.allocate(
                            (sizeBytes - (blockNumber * blockDev.blockSize))
                                .coerceAtMost(blockDev.blockSize.toLong() * bufferBlocks)
                                .toInt()
                        )
                        blockDev.read(blockNumber, buffer)
                        buffer.flip()
                        mBlockChannel.send(Pair(blockNumber, buffer))

                    } catch (e: ClosedSendChannelException) {
                        // Channel closed, stop
                        break
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Exception in I/O thread while reading blocks from $blockNumber to ${blockNumber + bufferBlocks}",
                            e
                        )
                        throw e
                    }
                }
            } catch (e: Exception) {
                mBlockChannel.close(e)
            } finally {
                mIoThreadRunning.set(false)
            }
        }

        rendezvous.receive()
    }

    private suspend fun setPosition(position: Long) {
        require(position >= 0) { "Position must be >= 0" }

        val newPosition = position.coerceAtMost(sizeBytes)

        if (newPosition == sizeBytes) {
            // EOF
            mCurrentBlockOffset = blockDev.blocks
            mReadBuffer = ByteBuffer.allocate(0)
            return
        }

        val newBlockOffset = newPosition / blockDev.blockSize
        val newByteOffset = newPosition % blockDev.blockSize

        // See if the new offset is within the current buffer
        if (::mReadBuffer.isInitialized) {
            if (newBlockOffset in mCurrentBlockOffset until mCurrentBlockOffset + bufferBlocks) {
                val bufferByteOffset = (newBlockOffset - mCurrentBlockOffset) * blockDev.blockSize + newByteOffset
                mReadBuffer.position(bufferByteOffset.toInt())
                return
            }
        }

        // Try finding the desired block in the channel
        var blockResult = tryGetBufferNonBlocking(newBlockOffset)

        // If it's not there, request it explicitly
        if (blockResult == null) {
            mReadNextBlockNumber.set(newBlockOffset)
            ensureIoThread()
            blockResult = waitAndGetBuffer(newBlockOffset)
        }

        val (blockNumber, buffer) = blockResult

        // Recompute the byte offset and set it as the buffer position
        val bufferByteOffset = (newBlockOffset - blockNumber) * blockDev.blockSize + newByteOffset
        buffer.position(bufferByteOffset.toInt())

        mCurrentBlockOffset = blockNumber
        mReadBuffer = buffer
    }

    private suspend fun ensureBuffer() {
        if (!::mReadBuffer.isInitialized) setPosition(mCurrentOffset)
        if (!isEOF && mReadBuffer.remaining() == 0) loadNextBuffer()
    }

    private suspend fun loadNextBuffer() {
        setPosition((mCurrentBlockOffset + bufferBlocks) * blockDev.blockSize)
    }

    override suspend fun seekAsync(offset: Long): Long {
        if (offset == 0L) return 0L

        val actualSkipDistance = when {
            mCurrentOffset + offset > sizeBytes -> sizeBytes - mCurrentOffset
            mCurrentOffset + offset < 0 -> -mCurrentOffset
            else -> offset
        }

        val position = mCurrentOffset + actualSkipDistance

        if (position < 0) throw IOException("Cannot seek to position $position")
        if (position > sizeBytes) throw IOException("End of stream reached")

        setPosition(position)

        return actualSkipDistance
    }

    override suspend fun skipAsync(n: Long): Long = seekAsync(n)

    override suspend fun readAsync(): Int {
        if (isEOF) return -1

        ensureBuffer()

        val result = mReadBuffer.get().toInt() and 0xFF

        if (mReadBuffer.remaining() == 0) loadNextBuffer()

        return result
    }

    override suspend fun readAsync(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        if (isEOF) return -1

        ensureBuffer()

        val bytesToRead = minOf(len.toLong(), sizeBytes - mCurrentByteOffset, b.size.toLong() - off)
        var bytesRead = 0L

        while (bytesRead < bytesToRead) {
            if (isEOF) break

            val bytesRemaining = bytesToRead - bytesRead
            val bytesInBuffer = mReadBuffer.remaining().toLong()
            val bytesToCopy = minOf(bytesRemaining, bytesInBuffer).toInt()

            mReadBuffer.get(b, (off + bytesRead).toInt(), bytesToCopy)
            bytesRead += bytesToCopy

            if (bytesRead < bytesToRead) loadNextBuffer()
        }

        return bytesRead.toInt()
    }

    override suspend fun readAsync(b: ByteArray): Int = readAsync(b, 0, b.size)

    override fun available(): Int {
        if (!::mReadBuffer.isInitialized) return 0
        return mReadBuffer.remaining()
    }

    override suspend fun closeAsync() {
        setPosition(sizeBytes) // Move to EOF
        mBlockChannel.close()

        try {
            while (true) {
                mBlockChannel.receive()
            }
        } catch (e: ClosedReceiveChannelException) {
            // Ignore
        }
    }

    override fun mark(readlimit: Int) {
        markedPosition = mCurrentOffset
    }

    override suspend fun resetAsync() = markedPosition.let {
        if (it == null) throw IOException("Stream not marked")
        setPosition(it)
    }

    override fun markSupported(): Boolean = true
    override suspend fun markSupportedAsync(): Boolean = true
}
