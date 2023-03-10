package eu.depau.etchdroid.utils.exception

import android.content.Context
import eu.depau.etchdroid.R
import eu.depau.etchdroid.utils.exception.base.RecoverableException
import eu.depau.etchdroid.utils.ktexts.rootCause
import eu.depau.etchdroid.utils.ktexts.toHRSize
import kotlinx.parcelize.Parcelize
import me.jahnen.libaums.libusbcommunication.LibusbError.NO_DEVICE
import me.jahnen.libaums.libusbcommunication.LibusbException

@Parcelize
open class UsbCommunicationException(
    override val cause: Throwable? = null,
) : RecoverableException(
    "Communication failed", cause
) {
    override fun getUiMessage(context: Context): String {
        // TODO extract to string resources
        val rootCause = rootCause
        if (rootCause is LibusbException) {
            val error = rootCause.libusbError
            if (error == NO_DEVICE) {
                return context.getString(R.string.the_usb_drive_was_unplugged)
            }
        }
        return context.getString(R.string.the_usb_drive_stopped_responding)
    }
}