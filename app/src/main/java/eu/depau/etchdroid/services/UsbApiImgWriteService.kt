package eu.depau.etchdroid.services

import android.hardware.usb.UsbDevice
import android.net.Uri
import eu.depau.etchdroid.utils.ktexts.getFileName
import eu.depau.etchdroid.utils.ktexts.getFileSize
import eu.depau.etchdroid.utils.ktexts.name
import java.io.InputStream

class UsbApiImgWriteService : UsbApiWriteService("UsbApiImgWriteService") {
    override fun getSendProgress(usbDevice: UsbDevice, uri: Uri): (Long) -> Unit {
        val imageSize = uri.getFileSize(this)
        return { bytes ->
            updateNotification(usbDevice.name, uri.getFileName(this), bytes, (bytes.toFloat() / imageSize * 100).toInt())
        }
    }

    override fun getInputStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri)!!
    }
}