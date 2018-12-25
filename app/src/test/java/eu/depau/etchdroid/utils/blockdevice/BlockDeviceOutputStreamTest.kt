package eu.depau.etchdroid.utils.blockdevice

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.driver.file.FileBlockDeviceDriver
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class BlockDeviceOutputStreamTest {
    @Test
    fun testWithCommonParams() {
        runWriteTest(
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
    }

    @Test
    fun testUnbuffered() {
        runWriteTest(
                10L * 1024 * 1024,  // 10 MiB
                512,
                1
        )
    }

    fun runWriteTest(testDevSize: Long, testBlockSize: Int, testBufferBlocks: Int) {
        val testFile = createTestFile(testDevSize)

        try {
            val testDev = createTestBlockDevice(testFile, testBlockSize)
            val outputStream = BlockDeviceOutputStream(testDev, bufferBlocks = testBufferBlocks)

            val deviceRandomAccessFile: RandomAccessFile = testDev::class.java
                    .getDeclaredField("file")
                    .apply { isAccessible = true }
                    .get(testDev) as RandomAccessFile
            var byteArray: ByteArray


            // Write some bytes
            outputStream.write(0)
            outputStream.write(1)
            outputStream.write(2)
            outputStream.write(3)
            outputStream.flush()
            deviceRandomAccessFile.fd.sync()

            byteArray = ByteArray(8)
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
            outputStream.write(byteArray)
            outputStream.flush()
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
                    (testBlockSize until testBlockSize + 8).map { (0xFE - it % 0xFF).toByte() }.toByteArray(),
                    byteArray.copyOfRange(testBlockSize, testBlockSize + 8)
            )

            // Fill the buffer except for the last 8 bytes
            // Note that flush successfully wrote one full block, so the block offset is now 1
            byteArray = ByteArray(testBufferBlocks * testBlockSize - 8)
            outputStream.write(byteArray)

            // Now do a write that goes out of the buffer
            byteArray = (0 until 16).map { 42.toByte() }.toByteArray()
            outputStream.write(byteArray)
            outputStream.flush()
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
            outputStream.write(byteArray)

            // This should work
            byteArray = ByteArray(4)
            outputStream.write(byteArray)

            // This should not
            try {
                outputStream.write(0)
                Assert.fail("Did not throw exception on EOF")
            } catch (e: IOException) {
                Assert.assertEquals("No space left on device", e.message)
            }

        } finally {
            testFile.delete()
        }
    }

    companion object {
        fun createTestFile(size: Long): File {
            val file = File.createTempFile("etchdroid-test-", ".img")
            val fileOutputStream = file.outputStream()

            // Writes 0xFE, 0xFD, ..., 0x00
            for (i in 0..size)
                fileOutputStream.write(0xFE - i.toInt() % 0xFF)
            fileOutputStream.flush()
            file.deleteOnExit()
            return file
        }

        fun createTestBlockDevice(file: File, blockSize: Int): BlockDeviceDriver =
                FileBlockDeviceDriver(file, blockSize, 0)
    }
}