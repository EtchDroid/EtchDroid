package eu.depau.etchdroid.kotlin_exts

import android.hardware.usb.UsbDevice
import android.os.Build

fun formatID(id: Int): String = Integer.toHexString(id).padStart(4, '0')

val UsbDevice.vidpid: String
    get() = "${formatID(this.vendorId)}:${formatID(this.productId)}"


val UsbDevice.name: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        "${this.manufacturerName} ${this.productName}"
    } else {
        this.deviceName
    }