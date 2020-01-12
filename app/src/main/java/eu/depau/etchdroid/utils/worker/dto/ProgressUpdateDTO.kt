package eu.depau.etchdroid.utils.worker.dto

import android.os.Parcel
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.kotlet.android.parcelable.*

data class ProgressUpdateDTO(
        val progress: Double?,
        val rate: Double?,
        val rateUnit: RateUnit?,
        val message: StringResBuilder? = null
) : KotletParcelable {
    constructor(parcel: Parcel) : this(
            progress = parcel.readNullable { parcel.readDouble() },
            rate = parcel.readNullable { parcel.readDouble() },
            rateUnit = parcel.readNullable { parcel.readEnum<RateUnit>() },
            message = parcel.readNullable {
                (parcel.readParcelable(StringResBuilder::class.java.classLoader) as StringResBuilder?)!!
            }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeNullable(progress) { writeDouble(it) }
            writeNullable(rate) { writeDouble(it) }
            writeNullable(rateUnit) { writeEnum(rateUnit) }
            writeNullable(message) { writeParcelable(message, flags) }
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::ProgressUpdateDTO)
    }
}