package eu.depau.etchdroid.services

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.util.Log
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.etchdroid.exceptions.UsbWriteException
import eu.depau.etchdroid.kotlinexts.getFileName
import eu.depau.etchdroid.kotlinexts.name
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer

abstract class UsbApiWriteService(name: String) : UsbWriteService(name) {
    // 512 * 32 bytes = USB max transfer size
    val ddBlocksize = 512 * 32 * 64  // 1 MB

    class Action {
        val WRITE_IMAGE = "eu.depau.etchdroid.action.API_WRITE_IMAGE"
        val WRITE_CANCEL = "eu.depau.etchdroid.action.API_WRITE_CANCEL"
    }

    abstract fun getSendProgress(usbDevice: UsbDevice, uri: Uri): (Long) -> Unit
    abstract fun getInputStream(uri: Uri): InputStream

    private fun getUsbMSDevice(usbDevice: UsbDevice): UsbMassStorageDevice? {
        val msDevs = UsbMassStorageDevice.getMassStorageDevices(this)

        for (dev in msDevs)
            if (dev.usbDevice == usbDevice)
                return dev

        return null
    }

    fun writeInputStream(inputStream: InputStream, msDev: UsbMassStorageDevice, sendProgress: (Long) -> Unit): Long {
        val blockDev = msDev.blockDevice
        val bsFactor = ddBlocksize / blockDev.blockSize
        val buffIS = BufferedInputStream(inputStream)
        val byteBuffer = ByteBuffer.allocate(blockDev.blockSize * bsFactor)

        var lastReadBytes: Int
        var readBytes = 0
        var readBlocksBytes = 0
        var offset = 0L
        var writtenBytes: Long = 0
        var remaining = 0

        while (true) {
            wakeLock(true)
            lastReadBytes = buffIS.read(byteBuffer.array()!!, remaining, byteBuffer.array().size - remaining)
            if (lastReadBytes < 0 && readBytes > 0) {
                // EOF, pad with some extra bits until next block
                if (readBytes % blockDev.blockSize > 0)
                    readBytes += blockDev.blockSize - (readBytes % blockDev.blockSize)
            } else if (lastReadBytes < 0) {
                // EOF, we've already written everything
                break
            } else {
                readBytes += lastReadBytes
            }

            byteBuffer.position(0)

            // Ensure written content size is a multiple of the block size
            remaining = readBytes % blockDev.blockSize
            readBlocksBytes = readBytes - remaining
            byteBuffer.limit(readBlocksBytes)

            // Write the buffer to the device
            try {
                blockDev.write(offset, byteBuffer)
            } catch (e: Exception) {
                throw UsbWriteException(offset, writtenBytes, e)
            }
            offset += (readBlocksBytes) / blockDev.blockSize
            writtenBytes += readBlocksBytes

            // Copy remaining bytes to the beginning of the buffer
            for (i in 0 until remaining)
                byteBuffer.array()[i] = byteBuffer.array()[readBlocksBytes + i]

            readBytes = remaining

            sendProgress(writtenBytes)
        }

        return writtenBytes
    }

    override fun writeImage(intent: Intent): Long {
        val uri: Uri = intent.data!!
        val inputStream = getInputStream(uri)

        val usbDevice: UsbDevice = intent.getParcelableExtra("usbDevice")
        val msDev = getUsbMSDevice(usbDevice)!!
        msDev.init()

        val sendProgress = getSendProgress(usbDevice, uri)
        val startTime = System.currentTimeMillis()
        var writtenBytes: Long = 0


        try {
            writtenBytes = writeInputStream(inputStream, msDev, sendProgress)

            resultNotification(usbDevice.name, uri.getFileName(this)!!, null, writtenBytes, startTime)
        } catch (e: Exception) {
            resultNotification(usbDevice.name, uri.getFileName(this)!!, e, writtenBytes, startTime)
            Log.e(tag, "Couldn't write image to ${usbDevice.name}")
            throw e
        } finally {
            wakeLock(false)
            msDev.close()
        }

        Log.d(tag, "Written $writtenBytes bytes to ${usbDevice.name} using API")
        return writtenBytes
    }
}