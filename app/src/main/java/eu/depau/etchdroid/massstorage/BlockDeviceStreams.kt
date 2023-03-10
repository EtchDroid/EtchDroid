package eu.depau.etchdroid.massstorage

import android.util.Log
import eu.depau.etchdroid.utils.AsyncOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

private const val TAG = "BlockDeviceInputStream"

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
    private val bufferBlocks: Int = 2048,
    private val coroutineScope: CoroutineScope? = null,
) : AsyncOutputStream(), ISeekableStream {

    private var byteBuffer = ByteBuffer.allocate(blockDev.blockSize * bufferBlocks)

    private var currentBlockOffset: Long = 0
    private val currentByteOffset: Long
        get() = currentBlockOffset * blockDev.blockSize + byteBuffer.position()

    private val sizeBytes: Long
        get() = blockDev.numBlocks * blockDev.blockSize

    private val bytesUntilEOF: Long
        get() = blockDev.numBlocks * blockDev.blockSize - currentByteOffset

    private var _ioThreadException: Exception? = null
    private suspend inline fun launchInIoThread(crossinline block: suspend () -> Unit) {
        if (_ioThreadException != null) {
            val e = _ioThreadException
            _ioThreadException = null
            throw e!!
        }

        if (coroutineScope == null) {
            block()
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    block()
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in IO thread", e)
                    _ioThreadException = e
                }
            }
        }
    }

    private val bufferMutex = Mutex()
    private val deviceMutex = Mutex()
    private val flushMutex = Mutex()

    override suspend fun closeAsync() {
        flushAsync()

        // Prevent further writes
        bufferMutex.lock()
        deviceMutex.lock()
        flushMutex.lock()
    }

    override suspend fun writeAsync(b: Int) = bufferMutex.withLock {
        if (bytesUntilEOF < 1) {
            flushBlockingNonLocking()
            throw IOException("No space left on device")
        }

        byteBuffer.put(b.toByte())
        swapBuffersAndFlushAsyncNonLocking()
    }

    override suspend fun writeAsync(b: ByteArray) {
        writeAsync(b, 0, b.size)
    }

    override suspend fun writeAsync(b: ByteArray, off: Int, len: Int) = bufferMutex.withLock {
        if (len <= 0 || off > b.size)
            return

        if (bytesUntilEOF < len) {
            flushBlockingNonLocking()
            throw IOException("No space left on device")
        }

        val actualLen = len.coerceAtMost(b.size - off)
        var toWrite = actualLen

        while (toWrite > 0) {
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            val toWriteNow = Math.min(toWrite, byteBuffer.remaining())
            byteBuffer.put(b, off + actualLen - toWrite, toWriteNow)
            toWrite -= toWriteNow

            if (toWrite > 0 && !byteBuffer.hasRemaining())
                flushBlockingNonLocking()
        }

        swapBuffersAndFlushAsyncNonLocking()
    }

    override suspend fun flushAsync() = bufferMutex.withLock {
        flushBlockingNonLocking()
    }

    // "Non-locking" w.r.t. the buffer mutex
    private suspend fun swapBuffersAndFlushAsyncNonLocking() {
        if (byteBuffer.hasRemaining())
            return

        // The lock will continue to be held after this call returns, until the data has been
        // written to the device
        flushMutex.lock()

        // Swap buffers
        val blockOffset = currentBlockOffset
        val oldByteBuffer = byteBuffer

        currentBlockOffset += oldByteBuffer.position() / blockDev.blockSize
        byteBuffer = ByteBuffer.allocate(byteBuffer.capacity()).apply {
            position(0)
            if (currentBlockOffset >= blockDev.numBlocks - bufferBlocks)
                limit(
                    blockDev.blockSize * (blockDev.numBlocks - currentBlockOffset).toInt()
                )
            else
                limit(capacity())
        }

        val synchronizationMutex = Mutex(locked = true)
        launchInIoThread {
            deviceMutex.withLock {
                synchronizationMutex.unlock()
                blockDev.write(blockOffset, oldByteBuffer)
            }
            flushMutex.unlock()
        }

        // Prevent returning before the device lock has been taken
        synchronizationMutex.lock()
    }

    private suspend fun flushBlockingNonLocking() = flushMutex.withLock {
        if (byteBuffer.position() == 0)
            return

        byteBuffer.flip()

        val toWrite = byteBuffer.limit()
        val incompleteBlockFullBytes = toWrite % blockDev.blockSize
        val fullBlocks = (toWrite - incompleteBlockFullBytes) / blockDev.blockSize

        // Check if we're trying to flush while the last written block isn't full
        deviceMutex.withLock {
            if (incompleteBlockFullBytes > 0) {
                val incompleteBlockBuffer = ByteBuffer.allocate(blockDev.blockSize)

                // Load last block from device
                withContext(Dispatchers.IO) {
                    blockDev.read(currentBlockOffset + fullBlocks, incompleteBlockBuffer)
                }

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
            withContext(Dispatchers.IO) {
                blockDev.write(currentBlockOffset, byteBuffer)
            }
        }

        // Copy the incomplete block at the beginning, then push back the position
        byteBuffer.apply {
            position(fullBlocks * blockDev.blockSize)
            limit(toWrite)
            compact()
            clear()
            position(incompleteBlockFullBytes)
        }

        // Ensure the buffer is limited on EOF
        if (blockDev.numBlocks - currentBlockOffset < bufferBlocks)
            byteBuffer.limit(
                (blockDev.numBlocks - currentBlockOffset).toInt() * blockDev.blockSize
            )

        currentBlockOffset += fullBlocks
    }

    override suspend fun seekAsync(offset: Long): Long = bufferMutex.withLock {
        // Flush all the unwritten changes to disk
        flushBlockingNonLocking()

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
                deviceMutex.withLock {
                    withContext(Dispatchers.IO) {
                        blockDev.read(currentBlockOffset, this@apply)
                    }
                }
                clear()
                position(blockByteOffset)
            }
        }

        return actualSkipDistance
    }
}

// I could finish porting this to coroutines, but it works well enough as it is so it's gonna stay
// like this for now
class BlockDeviceInputStream(
    private val blockDev: BlockDeviceDriver,
    private val prefetchBlocks: Int = 2048,
    private val coroutineScope: CoroutineScope? = null,
) : InputStream() {
    private var neverFetched = true

    private val byteBuffer = ByteBuffer.allocate(blockDev.blockSize * prefetchBlocks)

    private var currentBlockOffset: Long = 0
    private val currentByteOffset: Long
        get() = currentBlockOffset * blockDev.blockSize + byteBuffer.position()

    private var markedBlock: Long = 0
    private var markedBufferPosition: Int = 0

    private val sizeBytes: Long
        get() = blockDev.numBlocks * blockDev.blockSize

    private fun isNextByteAfterEOF(): Boolean {
        if (byteBuffer.hasRemaining())
            return false
        return currentBlockOffset + 1 >= blockDev.numBlocks
    }

    private inline fun launchInIoThread(crossinline block: () -> Unit) {
        if (coroutineScope == null) {
            block()
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            delay(100)
            block()
        }
    }

    private var _locked = false
    private inline fun <T> synchronizedIo(nestedLockAllowed: Boolean = false, block: () -> T): T {
        if (coroutineScope == null) {
            // This is just to spot any nested locks while testing
            require(!_locked || nestedLockAllowed) { "Buffer is already locked" }
            try {
                if (!nestedLockAllowed)
                    _locked = true
                return block()
            } finally {
                if (!nestedLockAllowed)
                    _locked = false
            }
        }
        synchronized(byteBuffer) {
            return block()
        }
    }

    override fun close() {
        super.close()
    }

    private fun fetch() {
        byteBuffer.clear()

        // Ensure the buffer is limited on EOF
        if (blockDev.numBlocks - currentBlockOffset < prefetchBlocks)
            byteBuffer.limit(
                (blockDev.numBlocks - currentBlockOffset).toInt() * blockDev.blockSize
            )

        blockDev.read(currentBlockOffset, byteBuffer)
        byteBuffer.flip()
    }

    private val isStarved: Boolean
        get() = neverFetched || !byteBuffer.hasRemaining()

    private fun fetchNextIfNeeded(blocking: Boolean = true) {
        if (!blocking) {
            launchInIoThread {
                synchronizedIo(nestedLockAllowed = true) {
                    fetchNextIfNeeded(blocking = true)
                }
            }
            return
        }

        if (neverFetched) {
            fetch()
            neverFetched = false
            return
        }
        if (byteBuffer.hasRemaining())
            return
        val newOffset = currentBlockOffset + byteBuffer.position() / blockDev.blockSize
        if (newOffset >= blockDev.numBlocks) {
            currentBlockOffset = newOffset
            return
        }

        currentBlockOffset = newOffset
        fetch()
    }

    override fun read(): Int = synchronizedIo {
        if (isNextByteAfterEOF())
            return -1
        fetchNextIfNeeded()
        return byteBuffer.get().toInt() and 0xFF
    }

    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int = synchronizedIo {
        if (isNextByteAfterEOF())
            return -1

        if (len <= 0 || off > b.size)
            return 0

        val actualLen = len.coerceAtMost(b.size - off)

        if (neverFetched) {
            fetch()
            neverFetched = false
        }

        var bytesRead = 0
        while (bytesRead < actualLen) {
            if (isStarved) {
                fetchNextIfNeeded()
            }
            if (isNextByteAfterEOF())
                break
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            if (byteBuffer.hasRemaining()) {
                val readable = Math.min(byteBuffer.remaining(), actualLen - bytesRead)
                byteBuffer.get(b, off + bytesRead, readable)
                bytesRead += readable
            }
        }
        fetchNextIfNeeded(blocking = false)
        return bytesRead
    }

    override fun skip(n: Long): Long = synchronizedIo {
        val actualSkipDistance = when {
            currentByteOffset + n > sizeBytes -> sizeBytes - currentByteOffset
            currentByteOffset + n < 0 -> -currentByteOffset
            else -> n
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

    override fun available(): Int = synchronizedIo {
        return byteBuffer.remaining()
    }

    override fun mark(readlimit: Int) = synchronizedIo {
        markedBlock = currentBlockOffset
        markedBufferPosition = byteBuffer.position()
    }

    override fun markSupported(): Boolean {
        return true
    }

    override fun reset(): Unit = synchronizedIo {
        currentBlockOffset = markedBlock
        fetch()
        byteBuffer.position(markedBufferPosition)
    }
}