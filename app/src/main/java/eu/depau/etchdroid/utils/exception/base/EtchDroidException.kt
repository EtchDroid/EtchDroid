package eu.depau.etchdroid.utils.exception.base

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.IOException

abstract class EtchDroidException(override val message: String, override val cause: Throwable? = null) :
    IOException(message, cause),
    Parcelable {
        abstract fun getUiMessage(context: Context): String
    }
