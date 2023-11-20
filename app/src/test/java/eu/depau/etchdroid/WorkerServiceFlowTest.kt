package eu.depau.etchdroid

import eu.depau.etchdroid.service.BUFFER_BLOCKS_SIZE
import eu.depau.etchdroid.service.WorkerServiceFlowImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.Random

@ExperimentalCoroutinesApi
class WorkerServiceFlowTest {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private fun testBasicWriteVerifyFlow(devSize: Int, blockSize: Int) = runBlocking {
        val random = Random()

        // Generate random image
        val image = ByteArray(devSize / 2 + random.nextInt(devSize / 2))
        random.nextBytes(image)

        val blockDev = MemoryBufferBlockDeviceDriver(devSize, blockSize)

        assertDoesNotThrow {
            WorkerServiceFlowImpl.writeImage(
                image.inputStream(),
                blockDev,
                image.size.toLong(),
                BUFFER_BLOCKS_SIZE * blockSize,
                0L,
                coroutineScope,
                grabWakeLock = {},
                sendProgressUpdate = { _, _, _, _ -> }
            )
        }

        assertDoesNotThrow {
            WorkerServiceFlowImpl.verifyImage(
                image.inputStream(),
                blockDev,
                image.size.toLong(),
                BUFFER_BLOCKS_SIZE * blockSize,
                coroutineScope,
                sendProgressUpdate = { _, _, _, _ -> },
                isVerificationCanceled = { false },
                grabWakeLock = {}
            )
        }
    }

    @Test
    fun testBasicWriteVerifyFlow512() = testBasicWriteVerifyFlow(1024 * 1024 * 512, 512)

    @Test
    fun testBasicWriteVerifyFlow4096() = testBasicWriteVerifyFlow(1024 * 1024 * 512, 4096)

    @Test
    fun testBasicWriteVerifyFlow733() = testBasicWriteVerifyFlow(1024 * 512 * 733, 733)

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