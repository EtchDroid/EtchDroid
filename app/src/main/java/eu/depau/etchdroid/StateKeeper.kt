package eu.depau.etchdroid

import android.hardware.usb.UsbDevice
import android.net.Uri
import com.github.mjdev.libaums.UsbMassStorageDevice
import eu.depau.etchdroid.enums.FlashMethod
import eu.depau.etchdroid.img_types.Image

object StateKeeper {
    var flashMethod: FlashMethod? = null
    var imageFile: Uri? = null
    var imageRepr: Image? = null

    var usbDevice: UsbDevice? = null
    var usbMassStorageDevice: UsbMassStorageDevice? = null
}