package eu.depau.etchdroid.utils.exception.base

import eu.depau.etchdroid.utils.ktexts.rootCause
import me.jahnen.libaums.libusbcommunication.LibusbError
import me.jahnen.libaums.libusbcommunication.LibusbException

abstract class RecoverableException(message: String, cause: Throwable? = null) :
    EtchDroidException(message, cause)

val RecoverableException.isUnplugged: Boolean
    get() = rootCause is LibusbException && (rootCause as LibusbException).libusbError == LibusbError.NO_DEVICE
