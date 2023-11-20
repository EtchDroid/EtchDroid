@file:OptIn(ExperimentalCoroutinesApi::class)

package eu.depau.etchdroid

import eu.depau.etchdroid.massstorage.BlockDeviceInputStream
import eu.depau.etchdroid.massstorage.BlockDeviceOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.lang.Math.min
import java.nio.ByteBuffer

@ExperimentalCoroutinesApi
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
            10 * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
    }

    @Test
    fun testWithWeirdBlockSize() {
        runReadTest(
            10 * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
        runPseudoRandomReadTest(
            10 * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
    }

    @Test
    fun testNoPrefetch() {
        runReadTest(
            10 * 1024 * 1024,  // 10 MiB
            512,
            1
        )
        runPseudoRandomReadTest(
            10 * 1024 * 1024,  // 10 MiB
            512,
            1
        )
    }

    private fun runReadTest(testDevSize: Long, testBlockSize: Int, testPrefetchBlocks: Int) {
        val testDev = MemoryBufferBlockDeviceDriver(testDevSize.toInt(), testBlockSize).apply {
            fillWithGrowingSequence()
        }
        val inputStream =
            BlockDeviceInputStream(testDev, prefetchBlocks = testPrefetchBlocks, coroutineScope)

        // Ensure that read(byteArray) fetches
        val byteArray0 = ByteArray(4)
        assertEquals(4, inputStream.read(byteArray0))
        assertArrayEquals(
            byteArrayOf(0, 1, 2, 3),
            byteArray0
        )
        assertEquals(4, inputStream.read(byteArray0))
        assertArrayEquals(
            byteArrayOf(4, 5, 6, 7),
            byteArray0
        )
        inputStream.skip(-8)

        // Read one block
        val byteArray1 = ByteArray(testBlockSize)
        assertEquals(testBlockSize, inputStream.read(byteArray1))
        assertArrayEquals(
            (0 until testBlockSize).map { (it % 0xFF).toByte() }.toByteArray(),
            byteArray1
        )

        // Read another block
        assertEquals(testBlockSize, inputStream.read(byteArray1))
        assertArrayEquals(
            (testBlockSize until testBlockSize * 2).map { (it % 0xFF).toByte() }.toByteArray(),
            byteArray1
        )

        // Rewind
        inputStream.skip((-2 * testBlockSize).toLong())

        // Read first bytes
        assertEquals(0, inputStream.read())
        assertEquals(1, inputStream.read())
        assertEquals(2, inputStream.read())
        assertEquals(3, inputStream.read())

        // Seek within prefetched buffer
        val skipBytes1: Long = 2L * 0xFF - 4
        assertEquals(skipBytes1, inputStream.skip(skipBytes1))

        assertEquals(0, inputStream.read())
        assertEquals(1, inputStream.read())
        assertEquals(2, inputStream.read())
        assertEquals(3, inputStream.read())

        // Seek outside prefetched buffer
        val skipBytes2 = 5L * 0xFF * testPrefetchBlocks - 4 + 100
        assertEquals(skipBytes2, inputStream.skip(skipBytes2))

        assertEquals(100 and 0xFF, inputStream.read())
        assertEquals(101 and 0xFF, inputStream.read())
        assertEquals(102 and 0xFF, inputStream.read())
        assertEquals(103 and 0xFF, inputStream.read())

        // Mark stream to get back here later
        // Implementation ignores readlimit so anything works
        inputStream.mark(0)

        // Seek to EOF
        val remainingBytes = testDevSize - (4 + skipBytes1 + 4 + skipBytes2 + 4)
        assertEquals(remainingBytes, inputStream.skip(remainingBytes * 20))

        // Ensure EOF
        assertEquals(-1, inputStream.read())

        // Seek to last byte (previous byte)
        @Suppress("KotlinConstantConditions")
        assertEquals(-1, inputStream.skip(-1))
        assertEquals((testDevSize - 1).toInt() % 0xFF, inputStream.read())

        // Go back to marked position
        inputStream.reset()
        assertEquals(104 and 0xFF, inputStream.read())
        assertEquals(105 and 0xFF, inputStream.read())
        assertEquals(106 and 0xFF, inputStream.read())
        assertEquals(107 and 0xFF, inputStream.read())

        // Go back to beginning
        inputStream.skip(-testDevSize)

        // Read to array
        var byteArray = ByteArray(8)
        assertEquals(byteArray.size, inputStream.read(byteArray))

        assertArrayEquals(
            byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
            byteArray
        )

        // Read to array with offset + length
        byteArray = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(4, inputStream.read(byteArray, 4, 4))
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 0, 8, 9, 10, 11),
            byteArray
        )

        // Read to array with length > array size
        assertEquals(4, inputStream.read(byteArray, 4, 200))
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 0, 12, 13, 14, 15),
            byteArray
        )

        // Read to array with offset outside of array
        assertEquals(0, inputStream.read(byteArray, 10, 4))

        // Go to end of prefetched blocks
        inputStream.skip(-testDevSize)
        inputStream.skip((testPrefetchBlocks * testBlockSize - 4).toLong())

        // Read to array, second half needs to be fetched
        assertEquals(byteArray.size, inputStream.read(byteArray))

        val currentPos = testPrefetchBlocks * testBlockSize - 4

        assertArrayEquals(
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
    }

    private fun runPseudoRandomReadTest(
        testDevSize: Int,
        testBlockSize: Int,
        testPrefetchBlocks: Int,
    ) {
        val testDev = MemoryBufferBlockDeviceDriver(testDevSize, testBlockSize).apply {
            fillWithRandom()
        }
        val inputStream = BlockDeviceInputStream(testDev, testPrefetchBlocks, coroutineScope)

        val byteBuffer = ByteBuffer.allocate(testDevSize)
        testDev.backingBuffer.copyInto(byteBuffer.array())
        byteBuffer.position(0)
        byteBuffer.limit(byteBuffer.capacity())

        @Suppress("UNUSED_VARIABLE")
        val originalBuffer = byteBuffer.duplicate()

        val byteArray = ByteArray(testBlockSize * testPrefetchBlocks)
        while (byteBuffer.hasRemaining()) {
            val readBytes = inputStream.read(byteArray)
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            assertEquals(
                min(byteBuffer.remaining(), byteArray.size),
                readBytes
            )

            val expectedBytes = ByteArray(readBytes)
            byteBuffer.get(expectedBytes)
            assertArrayEquals(expectedBytes, byteArray.copyOf(readBytes))
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp(): Unit {
            DebugProbes.install()
        }

        @JvmStatic
        @AfterAll
        fun tearDown(): Unit {
            DebugProbes.uninstall()
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
open class BlockDeviceOutputStreamTest {
    open val coroutineScope: CoroutineScope? = null

    @Test
    fun testWithCommonParams() {
        runWriteTest(
            10 * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
        runPseudoRandomWriteTest(
            10 * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
        runPseudoRandomSmallWritesTest(
            10 * 1024 * 1024,  // 10 MiB
            512,
            2048
        )
    }

    @Test
    fun testWithWeirdBlockSize() {
        runWriteTest(
            10 * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
        runPseudoRandomWriteTest(
            10 * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
        runPseudoRandomSmallWritesTest(
            10 * 666 * 2 * 666 * 2,  // 10 MegaDevils
            666,
            2048
        )
    }

    @Test
    fun testUnbuffered() {
        runWriteTest(
            10 * 1024 * 1024,  // 10 MiB
            512,
            1
        )
        runPseudoRandomWriteTest(
            10 * 1024 * 1024,  // 10 MiB
            512,
            1
        )
        runPseudoRandomSmallWritesTest(
            10 * 1024 * 1024,  // 10 MiB
            512,
            1
        )
    }


    private fun runWriteTest(testDevSize: Int, testBlockSize: Int, testBufferBlocks: Int) =
        runBlocking {
            val testDev = MemoryBufferBlockDeviceDriver(testDevSize, testBlockSize).apply {
                fillWithReverseGrowingSequence()
            }
            val outputStream =
                BlockDeviceOutputStream(
                    testDev, bufferBlocks = testBufferBlocks, coroutineScope = coroutineScope
                )

            // Write some bytes
            outputStream.apply {
                writeAsync(0)
                writeAsync(1)
                writeAsync(2)
                writeAsync(3)
                flushAsync()
            }

            var byteArray = ByteArray(8)
            testDev.backingBuffer.copyInto(byteArray, endIndex = 8)
            assertArrayEquals(
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

            byteArray = ByteArray(testBlockSize + 8)
            testDev.backingBuffer.copyInto(byteArray, endIndex = testBlockSize + 8)
            assertArrayEquals(
                byteArrayOf(
                    0, 1, 2, 3
                ),
                byteArray.copyOfRange(0, 4)
            )
            assertArrayEquals(
                (4 until testBlockSize).map { 0.toByte() }.toByteArray(),
                byteArray.copyOfRange(4, testBlockSize)
            )
            assertArrayEquals(
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

            val fullBufferOffset = (testBufferBlocks + 1) * testBlockSize

            val byteArray1 = ByteArray(20)
            testDev.backingBuffer.copyInto(
                byteArray1, 0, fullBufferOffset - 8, fullBufferOffset + 12
            )

            assertArrayEquals(
                byteArray,
                byteArray1.copyOfRange(0, 16)
            )
            assertArrayEquals(
                (fullBufferOffset + 8 until fullBufferOffset + 12)
                    .map { (0xFE - it % 0xFF).toByte() }
                    .toByteArray(),
                byteArray1.copyOfRange(16, 20)
            )

            // Go to end of file - 4 bytes
            val remainingBytes = testDevSize - (fullBufferOffset + 8)
            byteArray = ByteArray(remainingBytes - 4)
            outputStream.writeAsync(byteArray)

            // This should work
            byteArray = ByteArray(4)
            outputStream.writeAsync(byteArray)

            // This should not
            try {
                outputStream.writeAsync(0)
                fail("Did not throw exception on EOF")
            } catch (e: IOException) {
                assertEquals("No space left on device", e.message)
            }

        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runPseudoRandomWriteTest(
        testDevSize: Int,
        testBlockSize: Int,
        testBufferBlocks: Int,
    ) = runBlocking {
        val testDev = MemoryBufferBlockDeviceDriver(testDevSize, testBlockSize).apply {
            fillWithRandom()
        }
        val outputStream =
            BlockDeviceOutputStream(
                testDev, bufferBlocks = testBufferBlocks, coroutineScope = coroutineScope
            )

        val byteBuffer = ByteBuffer.allocate(testDevSize)
        testDev.backingBuffer.copyInto(byteBuffer.array())
        byteBuffer.position(0)
        byteBuffer.limit(byteBuffer.capacity())

        outputStream.writeAsync(byteBuffer.array())
        outputStream.flushAsync()

        val byteArray2 = ByteArray(testDevSize)
        testDev.backingBuffer.copyInto(byteArray2)
        assertArrayEquals(byteBuffer.array(), byteArray2)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runPseudoRandomSmallWritesTest(
        testDevSize: Int,
        testBlockSize: Int,
        testBufferBlocks: Int,
    ) = runBlocking {
        // Same as above, but instead of writing the entire buffer in one go, write it in small chunks of testBlockSize * testBufferBlocks
        val testDev = MemoryBufferBlockDeviceDriver(testDevSize, testBlockSize).apply {
            fillWithRandom()
        }
        val outputStream =
            BlockDeviceOutputStream(
                testDev, bufferBlocks = testBufferBlocks, coroutineScope = coroutineScope
            )

        val byteBuffer = ByteBuffer.allocate(testDevSize)
        testDev.backingBuffer.copyInto(byteBuffer.array())
        byteBuffer.position(0)
        byteBuffer.limit(byteBuffer.capacity())

        while (byteBuffer.hasRemaining()) {
            val byteArray =
                ByteArray(min(testBlockSize * testBufferBlocks, byteBuffer.remaining()))
            byteBuffer.get(byteArray)
            outputStream.writeAsync(byteArray)
        }
        outputStream.flushAsync()

        val byteArray2 = ByteArray(testDevSize)
        testDev.backingBuffer.copyInto(byteArray2)
        assertArrayEquals(byteBuffer.array(), byteArray2)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp(): Unit {
            DebugProbes.install()
        }

        @JvmStatic
        @AfterAll
        fun tearDown(): Unit {
            DebugProbes.uninstall()
        }
    }
}

class BlockDeviceInputStreamAsyncTest : BlockDeviceInputStreamTest() {
    override val coroutineScope = CoroutineScope(Dispatchers.IO)
}

class BlockDeviceOutputStreamAsyncTest : BlockDeviceOutputStreamTest() {
    override val coroutineScope = CoroutineScope(Dispatchers.IO)
}