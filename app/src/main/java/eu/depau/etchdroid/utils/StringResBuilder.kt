package eu.depau.etchdroid.utils

import android.content.Context
import android.os.Parcel
import eu.depau.kotlet.android.parcelable.KotletParcelable
import eu.depau.kotlet.android.parcelable.parcelableCreator
import java.io.Serializable

class StringResBuilder(private val resId: Int, private vararg val formatArgs: Any) : Serializable, KotletParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readArray(null) as Array<Any>
    )

    fun build(context: Context): String = if (formatArgs.isNotEmpty()) {
        context.getString(resId, formatArgs)
    } else {
        context.getString(resId)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeInt(resId)
            writeArray(formatArgs)
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::StringResBuilder)
    }
}