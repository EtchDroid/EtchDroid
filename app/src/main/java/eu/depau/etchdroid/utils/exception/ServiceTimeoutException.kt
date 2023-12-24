package eu.depau.etchdroid.utils.exception

import android.content.Context
import eu.depau.etchdroid.R
import eu.depau.etchdroid.utils.exception.base.FatalException
import kotlinx.parcelize.Parcelize

@Parcelize
class ServiceTimeoutException :
    FatalException("Timed out while waiting for service to start") {
    override fun getUiMessage(context: Context): String {
        return context.getString(R.string.service_timeout_message)
    }
}