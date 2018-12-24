package eu.depau.etchdroid.utils.blockdevice

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.driver.file.FileBlockDeviceDriver
import org.junit.Assert
import org.junit.Test
import java.io.File

class BlockDeviceInputStreamTest {
    @Test
    fun testWithCommonParams() {
        runReadTest(
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
    }

    @Test
    fun testNoPrefetch() {
        runReadTest(
                10L * 1024 * 1024,  // 10 MiB
                512,
                1
        )
    }

    fun runReadTest(testDevSize: Long, testBlockSize: Int, testPrefetchBlocks: Int) {
        val testFile = createTestFile(testDevSize)

        try {
            val testDev = createTestBlockDevice(testFile, testBlockSize)
            val inputStream = BlockDeviceInputStream(testDev, prefetchBlocks = testPrefetchBlocks)

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

        } finally {
            testFile.delete()
        }
    }

    companion object {
        fun createTestFile(size: Long): File {
            val file = File.createTempFile("etchdroid-test-", ".img")
            val fileOutputStream = file.outputStream()

            for (i in 0..size)
                fileOutputStream.write(i.toInt() % 0xFF)
            fileOutputStream.flush()
            file.deleteOnExit()
            return file
        }

        fun createTestBlockDevice(file: File, blockSize: Int): BlockDeviceDriver =
                FileBlockDeviceDriver(file, blockSize, 0)
    }
}