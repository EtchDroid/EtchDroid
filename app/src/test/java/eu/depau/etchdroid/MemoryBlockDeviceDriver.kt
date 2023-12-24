package eu.depau.etchdroid

import me.jahnen.libaums.core.driver.BlockDeviceDriver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.*
import java.nio.ByteBuffer
import java.util.Random


class MemoryBufferBlockDeviceDriver(
    private val size: Int,
    override val blockSize: Int,
) : BlockDeviceDriver {

    lateinit var backingBuffer: ByteArray
    override val blocks: Long
        get() = (backingBuffer.size / blockSize).toLong()

    // Used to simulate errors
    var exceptionToThrow: Function0<Throwable>? = null
        @Synchronized get
        @Synchronized set

    var throwAtBlockOffset: Long? = null
        @Synchronized get
        @Synchronized set

    init {
        init()
    }

    @Throws(IOException::class)
    override fun init() {
        if (size % blockSize != 0) {
            throw IOException("Memory buffer size must be a multiple of block size")
        }
        backingBuffer = ByteArray(size)
    }

    @Throws(IOException::class)
    override fun read(deviceOffset: Long, buffer: ByteBuffer) {
        if (throwAtBlockOffset != null && exceptionToThrow != null && deviceOffset >= throwAtBlockOffset!!) {
            val e = exceptionToThrow!!()
            exceptionToThrow = null
            throw e
        }

        val start = (deviceOffset * blockSize).toInt()
        val end = start + buffer.remaining()

        if (end > backingBuffer.size) {
            throw IOException("Read exceeds buffer size")
        }

        buffer.put(backingBuffer, start, buffer.remaining())
    }

    @Throws(IOException::class)
    override fun write(deviceOffset: Long, buffer: ByteBuffer) {
        if (throwAtBlockOffset != null && exceptionToThrow != null && deviceOffset >= throwAtBlockOffset!!) {
            val e = exceptionToThrow!!()
            exceptionToThrow = null
            throw e
        }

        val start = (deviceOffset * blockSize).toInt()
        val end = start + buffer.remaining()

        if (end > backingBuffer.size) {
            throw IOException("Write exceeds buffer size")
        }

        buffer.get(backingBuffer, start, buffer.remaining())
    }

    fun fillWithGrowingSequence() {
        for (i in 0 until size)
            backingBuffer[i] = (i % 0xFF).toByte()
    }

    fun fillWithReverseGrowingSequence() {
        for (i in 0 until size)
            backingBuffer[i] = (0xFE - i % 0xFF).toByte()
    }

    fun fillWithRandom(seed: Long? = null) {
        val random = if (seed != null) Random(seed) else Random()
        random.nextBytes(backingBuffer)
    }
}


class MemoryBufferBlockDeviceDriverTest {
    @Test
    fun testReadWrite() {
        val driver = MemoryBufferBlockDeviceDriver(2048, 512)
        driver.fillWithGrowingSequence()

        val writeBuffer = ByteBuffer.allocate(1024)
        val random = Random()
        random.nextBytes(writeBuffer.array())

        assertDoesNotThrow { driver.write(1, writeBuffer) }

        val readBuffer = ByteBuffer.allocate(1024)
        assertDoesNotThrow { driver.read(1, readBuffer) }

        readBuffer.flip()
        writeBuffer.flip()
        assertEquals(writeBuffer, readBuffer)
    }

    @Test
    fun testReadBeyondBuffer() {
        val driver = MemoryBufferBlockDeviceDriver(1024, 512)
        driver.init()

        val readBuffer = ByteBuffer.allocate(512)
        val exception = assertThrows<IOException> { driver.read(2, readBuffer) }
        assertTrue(exception.message!!.contains("exceeds buffer size"))

        val readBuffer1 = ByteBuffer.allocate(1024)
        val exception1 = assertThrows<IOException> { driver.read(1, readBuffer1) }
        assertTrue(exception1.message!!.contains("exceeds buffer size"))
    }
}