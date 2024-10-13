package eu.depau.etchdroid.utils.exception

import android.content.Context
import eu.depau.etchdroid.R
import eu.depau.etchdroid.utils.exception.base.FatalException
import kotlinx.parcelize.Parcelize

@Parcelize
class MissingDeviceException : FatalException("Worker service launched with null device") {
    override fun getUiMessage(context: Context): String {
        return context.getString(R.string.worker_null_device_error)
    }
}