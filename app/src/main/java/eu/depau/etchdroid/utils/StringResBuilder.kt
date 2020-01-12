package eu.depau.etchdroid.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import eu.depau.kotlet.android.parcelable.KotletParcelable
import eu.depau.kotlet.android.parcelable.parcelableCreator
import java.io.Serializable

class StringResBuilder(private val resId: Int, private vararg val formatArgs: Any) : Serializable, KotletParcelable {
    @SuppressLint("ParcelClassLoader")
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readArray(null) as Array<Any>
    )

    constructor(resId: Int) : this(resId, emptyArray<Any>())

    fun build(context: Context): String {
        if (formatArgs.isEmpty())
            return context.getString(resId)

        // Unwrap inner StringResBuilders
        val formatArgsUnwrapped = formatArgs.map {
            if (it is StringResBuilder)
                it.build(context)
            else
                it
        }.toTypedArray()

        return context.getString(resId, formatArgsUnwrapped)
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