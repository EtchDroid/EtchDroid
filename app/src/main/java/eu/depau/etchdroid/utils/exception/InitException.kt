package eu.depau.etchdroid.utils.exception

import android.content.Context
import eu.depau.etchdroid.R
import eu.depau.etchdroid.utils.exception.base.RecoverableException
import kotlinx.parcelize.Parcelize

@Parcelize
open class InitException(override val message: String, override val cause: Throwable? = null) :
    RecoverableException(message, cause) {

    override fun getUiMessage(context: Context): String {
        return context.getString(R.string.the_device_is_not_responding)
    }
}
