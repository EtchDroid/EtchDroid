package eu.depau.etchdroid

import android.hardware.usb.UsbDevice
import android.net.Uri

object StateKeeper {
    var imageFile: Uri? = null
    var usbDevice: UsbDevice? = null
    var libusbRegistered: Boolean = false
}