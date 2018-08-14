package eu.depau.ddroid.services

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.net.Uri
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.ddroid.abc.UsbWriteService
import eu.depau.ddroid.utils.getFileSize
import eu.depau.ddroid.utils.name
import java.nio.ByteBuffer

class UsbAPIWriteService : UsbWriteService("UsbAPIWriteService") {
    val DD_BLOCKSIZE = 4096

    class Action {
        val WRITE_IMAGE = "eu.depau.ddroid.action.API_WRITE_IMAGE"
        val WRITE_CANCEL = "eu.depau.ddroid.action.API_WRITE_CANCEL"
    }

    override fun onHandleIntent(intent: Intent?) {
        val uri: Uri = intent!!.data!!
        val usbDevice: UsbDevice = intent.getParcelableExtra("usbDevice")

        startForeground(FOREGROUND_ID, buildForegroundNotification(usbDevice.name, uri, -1, -1))

        try {
            val notify = { bytes: Long, total: Long -> updateNotification(usbDevice.name, uri, bytes, total) }
            writeImage(usbDevice, uri, notify)
        } finally {
            stopForeground(true)
        }
    }

    private fun getUsbMSDevice(usbDevice: UsbDevice): UsbMassStorageDevice? {
        val msDevs = UsbMassStorageDevice.getMassStorageDevices(this)

        for (dev in msDevs) {
            if (dev.usbDevice == usbDevice)
                return dev
        }

        return null
    }

    private fun writeImage(usbDevice: UsbDevice, uri: Uri, notify: (Long, Long) -> Unit): Long {
        val msDev = getUsbMSDevice(usbDevice)!!
        msDev.init()

        val blockDev = msDev.blockDevice
        var bsFactor = DD_BLOCKSIZE / blockDev.blockSize
        val byteBuffer = ByteBuffer.allocate(blockDev.blockSize * bsFactor)
        val imageSize = uri.getFileSize(this)
        val inputStream = contentResolver.openInputStream(uri)!!

        var readBytes: Int
        var offset = 0L

        while (true) {
            readBytes = inputStream.read(byteBuffer.array()!!)
            if (readBytes < 0)
                break
            byteBuffer.position(0)

            blockDev.write(offset, byteBuffer)
            offset++

            notify(offset * blockDev.blockSize * bsFactor, imageSize)
        }

        msDev.close()

        return offset * blockDev.blockSize
    }
}