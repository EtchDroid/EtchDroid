package eu.depau.etchdroid.utils.exception

import android.content.Context
import eu.depau.etchdroid.R
import eu.depau.etchdroid.utils.exception.base.FatalException
import kotlinx.parcelize.Parcelize

@Parcelize
class UnknownException(override val cause: Throwable) : FatalException("Unknown error", cause) {
    override fun getUiMessage(context: Context): String {
        return context.getString(R.string.an_unknown_error_occurred, cause.message)
    }
}