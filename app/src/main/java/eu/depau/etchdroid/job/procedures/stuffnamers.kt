package eu.depau.etchdroid.job.procedures

import android.content.Context
import android.hardware.usb.UsbDevice
import android.net.Uri
import eu.depau.etchdroid.R
import eu.depau.etchdroid.job.enums.TargetType
import eu.depau.etchdroid.job.enums.TargetType.*
import eu.depau.etchdroid.utils.ktexts.name
import eu.depau.kotlet.android.extensions.uri.getFilePath

/**
 * Utilities to give human-readable names to random stuff
 */

fun autoName(targetType: TargetType, targetDescriptor: Any, context: Context): String {
    return when (targetType) {
        ANDROID_URI   -> (targetDescriptor as Uri).getFileName(context)
        AUMS_BLOCKDEV -> (targetDescriptor as UsbDevice).name
        FS_FILE       -> getPathFileName(targetDescriptor as String, context)
    }
}


fun getPathFileName(path: String, context: Context): String {
    val cut = path.lastIndexOf('/')
    if (cut != -1) {
        return path.substring(cut + 1)
    }

    return context.getString(R.string.file_unknown)
}


fun Uri.getFileName(context: Context): String {
    return try {
        this.getFilePath(context)!!
    } catch (e: Exception) {
        getPathFileName(this.encodedPath ?: this.path ?: this.toString(), context)
    }
}