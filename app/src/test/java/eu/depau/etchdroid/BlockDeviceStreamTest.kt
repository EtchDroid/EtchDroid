package eu.depau.etchdroid

import eu.depau.etchdroid.massstorage.BlockDeviceInputStream
import eu.depau.etchdroid.massstorage.BlockDeviceOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.driver.file.FileBlockDeviceDriver
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.Math.min
import java.nio.ByteBuffer
import kotlin.random.Random

open class BlockDeviceInputStreamTest {
    open val coroutineScope: CoroutineScope? = null

    @Test
    fun testWithCommonParams() {
        runReadTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
        runPseudoRandomReadTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
    }

    @Test
    fun testWithWeirdBlockSize() {
        runReadTest(
            10L * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
        runPseudoRandomReadTest(
            10L * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
    }

    @Test
    fun testNoPrefetch() {
        runReadTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            1
        )
        runPseudoRandomReadTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            1
        )
    }

    fun runReadTest(testDevSize: Long, testBlockSize: Int, testPrefetchBlocks: Int) {
        val testFile = createTestFile(testDevSize.toInt())

        try {
            val testDev = createTestBlockDevice(testFile, testBlockSize)
            val inputStream =
                BlockDeviceInputStream(testDev, prefetchBlocks = testPrefetchBlocks, coroutineScope)

            // Ensure that read(byteArray) fetches
            val byteArray0 = ByteArray(4)
            Assert.assertEquals(4, inputStream.read(byteArray0))
            Assert.assertArrayEquals(
                byteArrayOf(0, 1, 2, 3),
                byteArray0
            )
            Assert.assertEquals(4, inputStream.read(byteArray0))
            Assert.assertArrayEquals(
                byteArrayOf(4, 5, 6, 7),
                byteArray0
            )
            inputStream.skip(-8)

            // Read one block
            val byteArray1 = ByteArray(testBlockSize)
            Assert.assertEquals(testBlockSize, inputStream.read(byteArray1))
            Assert.assertArrayEquals(
                (0 until testBlockSize).map { (it % 0xFF).toByte() }.toByteArray(),
                byteArray1
            )

            // Read another block
            Assert.assertEquals(testBlockSize, inputStream.read(byteArray1))
            Assert.assertArrayEquals(
                (testBlockSize until testBlockSize * 2).map { (it % 0xFF).toByte() }.toByteArray(),
                byteArray1
            )

            // Rewind
            inputStream.skip((-2 * testBlockSize).toLong())

            // Read first bytes
            Assert.assertEquals(0, inputStream.read())
            Assert.assertEquals(1, inputStream.read())
            Assert.assertEquals(2, inputStream.read())
            Assert.assertEquals(3, inputStream.read())

            // Seek within prefetched buffer
            val skipBytes1: Long = 2L * 0xFF - 4
            Assert.assertEquals(skipBytes1, inputStream.skip(skipBytes1))

            Assert.assertEquals(0, inputStream.read())
            Assert.assertEquals(1, inputStream.read())
            Assert.assertEquals(2, inputStream.read())
            Assert.assertEquals(3, inputStream.read())

            // Seek outside prefetched buffer
            val skipBytes2 = 5L * 0xFF * testPrefetchBlocks - 4 + 100
            Assert.assertEquals(skipBytes2, inputStream.skip(skipBytes2))

            Assert.assertEquals(100 and 0xFF, inputStream.read())
            Assert.assertEquals(101 and 0xFF, inputStream.read())
            Assert.assertEquals(102 and 0xFF, inputStream.read())
            Assert.assertEquals(103 and 0xFF, inputStream.read())

            // Mark stream to get back here later
            // Implementation ignores readlimit so anything works
            inputStream.mark(0)

            // Seek to EOF
            val remainingBytes = testDevSize - (4 + skipBytes1 + 4 + skipBytes2 + 4)
            Assert.assertEquals(remainingBytes, inputStream.skip(remainingBytes * 20))

            // Ensure EOF
            Assert.assertEquals(-1, inputStream.read())

            // Seek to last byte (previous byte)
            Assert.assertEquals(-1, inputStream.skip(-1))
            Assert.assertEquals((testDevSize - 1).toInt() % 0xFF, inputStream.read())

            // Go back to marked position
            inputStream.reset()
            Assert.assertEquals(104 and 0xFF, inputStream.read())
            Assert.assertEquals(105 and 0xFF, inputStream.read())
            Assert.assertEquals(106 and 0xFF, inputStream.read())
            Assert.assertEquals(107 and 0xFF, inputStream.read())

            // Go back to beginning
            inputStream.skip(-testDevSize)

            // Read to array
            var byteArray = ByteArray(8)
            Assert.assertEquals(byteArray.size, inputStream.read(byteArray))

            Assert.assertArrayEquals(
                byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
                byteArray
            )

            // Read to array with offset + length
            byteArray = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
            Assert.assertEquals(4, inputStream.read(byteArray, 4, 4))
            Assert.assertArrayEquals(
                byteArrayOf(0, 0, 0, 0, 8, 9, 10, 11),
                byteArray
            )

            // Read to array with length > array size
            Assert.assertEquals(4, inputStream.read(byteArray, 4, 200))
            Assert.assertArrayEquals(
                byteArrayOf(0, 0, 0, 0, 12, 13, 14, 15),
                byteArray
            )

            // Read to array with offset outside of array
            Assert.assertEquals(0, inputStream.read(byteArray, 10, 4))

            // Go to end of prefetched blocks
            inputStream.skip(-testDevSize)
            inputStream.skip((testPrefetchBlocks * testBlockSize - 4).toLong())

            // Read to array, second half needs to be fetched
            Assert.assertEquals(byteArray.size, inputStream.read(byteArray))

            val currentPos = testPrefetchBlocks * testBlockSize - 4

            Assert.assertArrayEquals(
                byteArrayOf(
                    ((currentPos + 0) % 0xFF).toByte(),
                    ((currentPos + 1) % 0xFF).toByte(),
                    ((currentPos + 2) % 0xFF).toByte(),
                    ((currentPos + 3) % 0xFF).toByte(),
                    ((currentPos + 4) % 0xFF).toByte(),
                    ((currentPos + 5) % 0xFF).toByte(),
                    ((currentPos + 6) % 0xFF).toByte(),
                    ((currentPos + 7) % 0xFF).toByte()
                ),
                byteArray
            )

        } finally {
            testFile.delete()
        }
    }

    private fun runPseudoRandomReadTest(
        testDevSize: Long,
        testBlockSize: Int,
        testPrefetchBlocks: Int,
    ) {
        val seed = System.currentTimeMillis()
        val testFile = createPseudoRandomTestFile(testDevSize.toInt(), seed)
        try {
            val testDev = createTestBlockDevice(testFile, testBlockSize)
            val inputStream = BlockDeviceInputStream(testDev, testPrefetchBlocks, coroutineScope)

            val byteBuffer = ByteBuffer.allocate(testDevSize.toInt())
            Random(seed).nextBytes(byteBuffer.array())
            byteBuffer.position(0)
            byteBuffer.limit(byteBuffer.capacity())

            @Suppress("UNUSED_VARIABLE")
            val originalBuffer = byteBuffer.duplicate()

            val byteArray = ByteArray(testBlockSize * testPrefetchBlocks)
            while (byteBuffer.hasRemaining()) {
                val readBytes = inputStream.read(byteArray)
                @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
                Assert.assertEquals(
                    min(byteBuffer.remaining(), byteArray.size),
                    readBytes
                )

                val expectedBytes = ByteArray(readBytes)
                byteBuffer.get(expectedBytes)
                Assert.assertArrayEquals(expectedBytes, byteArray.copyOf(readBytes))
            }
        } finally {
            testFile.delete()
        }
    }

    companion object {
        fun createTestFile(size: Int): File {
            val file = File.createTempFile("etchdroid-test-", ".img")
            val fileOutputStream = file.outputStream()

            val byteArray = ByteArray(size)
            for (i in 0 until size)
                byteArray[i] = (i % 0xFF).toByte()
            fileOutputStream.write(byteArray)

            fileOutputStream.flush()
            file.deleteOnExit()
            return file
        }

        fun createTestBlockDevice(file: File, blockSize: Int): BlockDeviceDriver =
            FileBlockDeviceDriver(file, 0, blockSize)
    }
}


fun createPseudoRandomTestFile(size: Int, seed: Long): File {
    val file = File.createTempFile("etchdroid-test-", ".img")
    val fileOutputStream = file.outputStream()

    val random = Random(seed)
    val byteArray = ByteArray(size)
    random.nextBytes(byteArray)
    fileOutputStream.write(byteArray)

    fileOutputStream.flush()
    file.deleteOnExit()
    return file
}

@Suppress("BlockingMethodInNonBlockingContext")
open class BlockDeviceOutputStreamTest {
    open val coroutineScope: CoroutineScope? = null

    @Test
    fun testWithCommonParams() {
        runWriteTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
        runPseudoRandomWriteTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
        runPseudoRandomSmallWritesTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
    }

    @Test
    fun testWithWeirdBlockSize() {
        runWriteTest(
            10L * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
        runPseudoRandomWriteTest(
            10L * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
        runPseudoRandomSmallWritesTest(
            10L * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
    }

    @Test
    fun testUnbuffered() {
        runWriteTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            1
        )
        runPseudoRandomWriteTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            1
        )
        runPseudoRandomSmallWritesTest(
            10L * 1024 * 1024,  // 10 MiB
            512,
            1
        )
    }


    private fun runWriteTest(testDevSize: Long, testBlockSize: Int, testBufferBlocks: Int) = runBlocking {
        val testFile = createTestFile(testDevSize.toInt())

        try {
            val testDev = createTestBlockDevice(testFile, testBlockSize)
            val outputStream =
                BlockDeviceOutputStream(testDev, bufferBlocks = testBufferBlocks, coroutineScope)

            val deviceRandomAccessFile: RandomAccessFile = testDev::class.java
                .getDeclaredField("file")
                .apply { isAccessible = true }
                .get(testDev) as RandomAccessFile

            var byteArray = ByteArray(8)

            // Write some bytes
            outputStream.apply {
                writeAsync(0)
                writeAsync(1)
                writeAsync(2)
                writeAsync(3)
                flushAsync()
            }
            deviceRandomAccessFile.fd.sync()

            RandomAccessFile(testFile, "r").use {
                it.read(byteArray)
            }

            Assert.assertArrayEquals(
                byteArrayOf(
                    0, 1, 2, 3, 0xFA.toByte(), 0xF9.toByte(), 0xF8.toByte(), 0xF7.toByte()
                ),
                byteArray
            )

            // Fill current block
            byteArray = ByteArray(testBlockSize - 4)
            outputStream.apply {
                writeAsync(byteArray)
                flushAsync()
            }
            deviceRandomAccessFile.fd.sync()

            byteArray = ByteArray(testBlockSize + 8)
            RandomAccessFile(testFile, "r").use {
                it.read(byteArray)
            }

            Assert.assertArrayEquals(
                byteArrayOf(
                    0, 1, 2, 3
                ),
                byteArray.copyOfRange(0, 4)
            )
            Assert.assertArrayEquals(
                (4 until testBlockSize).map { 0.toByte() }.toByteArray(),
                byteArray.copyOfRange(4, testBlockSize)
            )
            Assert.assertArrayEquals(
                (testBlockSize until testBlockSize + 8).map { (0xFE - it % 0xFF).toByte() }
                    .toByteArray(),
                byteArray.copyOfRange(testBlockSize, testBlockSize + 8)
            )

            // Fill the buffer except for the last 8 bytes
            // Note that flush successfully wrote one full block, so the block offset is now 1
            byteArray = ByteArray(testBufferBlocks * testBlockSize - 8)
            outputStream.writeAsync(byteArray)

            // Now do a write that goes out of the buffer
            byteArray = (0 until 16).map { 42.toByte() }.toByteArray()
            outputStream.apply {
                writeAsync(byteArray)
                flushAsync()
            }
            deviceRandomAccessFile.fd.sync()

            val fullBufferOffset = (testBufferBlocks + 1) * testBlockSize

            val byteArray1 = ByteArray(20)
            RandomAccessFile(testFile, "r").use {
                it.seek(fullBufferOffset - 8L)
                it.read(byteArray1)
            }

            Assert.assertArrayEquals(
                byteArray,
                byteArray1.copyOfRange(0, 16)
            )
            Assert.assertArrayEquals(
                (fullBufferOffset + 8 until fullBufferOffset + 12)
                    .map { (0xFE - it % 0xFF).toByte() }
                    .toByteArray(),
                byteArray1.copyOfRange(16, 20)
            )

            // Go to end of file - 4 bytes
            val remainingBytes = testDevSize - (fullBufferOffset + 8)
            byteArray = ByteArray(remainingBytes.toInt() - 4)
            outputStream.writeAsync(byteArray)

            // This should work
            byteArray = ByteArray(4)
            outputStream.writeAsync(byteArray)

            // This should not
            try {
                outputStream.writeAsync(0)
                Assert.fail("Did not throw exception on EOF")
            } catch (e: IOException) {
                Assert.assertEquals("No space left on device", e.message)
            }

        } finally {
            testFile.delete()
        }
    }

    private fun runPseudoRandomWriteTest(
        testDevSize: Long,
        testBlockSize: Int,
        testBufferBlocks: Int,
    ) = runBlocking {
        val seed = System.currentTimeMillis()
        val testFile = createPseudoRandomTestFile(testDevSize.toInt(), seed)

        try {
            val testDev = createTestBlockDevice(testFile, testBlockSize)
            val outputStream =
                BlockDeviceOutputStream(testDev, bufferBlocks = testBufferBlocks, coroutineScope)

            val deviceRandomAccessFile: RandomAccessFile = testDev::class.java
                .getDeclaredField("file")
                .apply { isAccessible = true }
                .get(testDev) as RandomAccessFile

            val random = Random(seed)

            val byteBuffer = ByteBuffer.allocate(testDevSize.toInt())
            random.nextBytes(byteBuffer.array())
            byteBuffer.position(0)
            byteBuffer.limit(byteBuffer.capacity())

            outputStream.writeAsync(byteBuffer.array())
            outputStream.flushAsync()
            deviceRandomAccessFile.fd.sync()

            val byteArray2 = ByteArray(testDevSize.toInt())
            RandomAccessFile(testFile, "r").use {
                it.read(byteArray2)
            }
            Assert.assertArrayEquals(byteBuffer.array(), byteArray2)
        } finally {
            testFile.delete()
        }
    }

    private fun runPseudoRandomSmallWritesTest(
        testDevSize: Long,
        testBlockSize: Int,
        testBufferBlocks: Int,
    ) = runBlocking {
        // Same as above, but instead of writing the entire buffer in one go, write it in small chunks of testBlockSize * testBufferBlocks
        val seed = System.currentTimeMillis()
        val testFile = createPseudoRandomTestFile(testDevSize.toInt(), seed)

        try {
            val testDev = createTestBlockDevice(testFile, testBlockSize)
            val outputStream =
                BlockDeviceOutputStream(testDev, bufferBlocks = testBufferBlocks, coroutineScope)

            val deviceRandomAccessFile: RandomAccessFile = testDev::class.java
                .getDeclaredField("file")
                .apply { isAccessible = true }
                .get(testDev) as RandomAccessFile

            val random = Random(seed)

            val byteBuffer = ByteBuffer.allocate(testDevSize.toInt())
            random.nextBytes(byteBuffer.array())
            byteBuffer.position(0)
            byteBuffer.limit(byteBuffer.capacity())

            while (byteBuffer.hasRemaining()) {
                val byteArray =
                    ByteArray(min(testBlockSize * testBufferBlocks, byteBuffer.remaining()))
                byteBuffer.get(byteArray)
                outputStream.writeAsync(byteArray)
            }
            outputStream.flushAsync()
            deviceRandomAccessFile.fd.sync()

            val byteArray2 = ByteArray(testDevSize.toInt())
            RandomAccessFile(testFile, "r").use {
                it.read(byteArray2)
            }
            Assert.assertArrayEquals(byteBuffer.array(), byteArray2)
        } finally {
            testFile.delete()
        }
    }

    companion object {
        fun createTestFile(size: Int): File {
            val file = File.createTempFile("etchdroid-test-", ".img")
            val fileOutputStream = file.outputStream()

            val byteArray = ByteArray(size)
            // Writes 0xFE, 0xFD, ..., 0x00
            for (i in 0 until size)
                byteArray[i] = (0xFE - i % 0xFF).toByte()
            fileOutputStream.write(byteArray)

            fileOutputStream.flush()
            file.deleteOnExit()
            return file
        }

        fun createTestBlockDevice(file: File, blockSize: Int): BlockDeviceDriver =
            FileBlockDeviceDriver(file, 0, blockSize)
    }
}


class BlockDeviceInputStreamAsyncTest : BlockDeviceInputStreamTest() {
    override val coroutineScope = CoroutineScope(Dispatchers.IO)
}

class BlockDeviceOutputStreamAsyncTest : BlockDeviceOutputStreamTest() {
    override val coroutineScope = CoroutineScope(Dispatchers.IO)
}