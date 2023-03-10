package eu.depau.etchdroid.utils.ktexts

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager

fun <T> Intent.safeParcelableExtra(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        return getParcelableExtra(key)
    }
}

inline fun <reified T> Intent.safeParcelableExtra(key: String): T? {
    return safeParcelableExtra(key, T::class.java)
}

val Intent.usbDevice: UsbDevice?
    get() = safeParcelableExtra(UsbManager.EXTRA_DEVICE)

fun Intent.broadcastLocally(context: Context) {
    LocalBroadcastManager.getInstance(context).sendBroadcast(this)
}

fun Intent.broadcastLocallySync(context: Context) {
    LocalBroadcastManager.getInstance(context).sendBroadcastSync(this)
}