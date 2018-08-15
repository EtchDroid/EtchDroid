package eu.depau.etchdroid.services

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.util.Log
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.etchdroid.abc.UsbWriteService
import eu.depau.etchdroid.utils.getFileName
import eu.depau.etchdroid.utils.getFileSize
import eu.depau.etchdroid.utils.name
import java.nio.ByteBuffer

class UsbAPIWriteService : UsbWriteService("UsbAPIWriteService") {
    // 512 * 32 bytes = USB max transfer size
    val DD_BLOCKSIZE = 512 * 32 * 64  // 1 MB

    class Action {
        val WRITE_IMAGE = "eu.depau.etchdroid.action.API_WRITE_IMAGE"
        val WRITE_CANCEL = "eu.depau.etchdroid.action.API_WRITE_CANCEL"
    }

    private fun getUsbMSDevice(usbDevice: UsbDevice): UsbMassStorageDevice? {
        val msDevs = UsbMassStorageDevice.getMassStorageDevices(this)

        for (dev in msDevs) {
            if (dev.usbDevice == usbDevice)
                return dev
        }

        return null
    }

    override fun writeImage(intent: Intent): Long {
        val uri: Uri = intent.data!!
        val usbDevice: UsbDevice = intent.getParcelableExtra("usbDevice")

        val msDev = getUsbMSDevice(usbDevice)!!
        msDev.init()

        val blockDev = msDev.blockDevice
        val bsFactor = DD_BLOCKSIZE / blockDev.blockSize
        val byteBuffer = ByteBuffer.allocate(blockDev.blockSize * bsFactor)
        val imageSize = uri.getFileSize(this)
        val inputStream = contentResolver.openInputStream(uri)!!

        val startTime = System.currentTimeMillis()

        var readBytes: Int
        var offset = 0L
        var writtenBytes: Long = 0

        try {
            while (true) {
                wakeLock(true)
                readBytes = inputStream.read(byteBuffer.array()!!)
                if (readBytes < 0)
                    break
                byteBuffer.position(0)

                blockDev.write(offset, byteBuffer)
                offset += bsFactor
                writtenBytes += readBytes

                updateNotification(usbDevice.name, uri.getFileName(this), offset * blockDev.blockSize, imageSize)
            }

            resultNotification(usbDevice.name, uri.getFileName(this)!!, true, writtenBytes, startTime)
        } catch (e: Exception) {
            resultNotification(usbDevice.name, uri.getFileName(this)!!, false, writtenBytes, startTime)
            Log.e(TAG, "Could't write image to ${usbDevice.name}")
            throw e
        } finally {
            wakeLock(false)
            msDev.close()
        }

        Log.d(TAG, "Written $writtenBytes bytes to ${usbDevice.name} using API")
        return writtenBytes
    }
}