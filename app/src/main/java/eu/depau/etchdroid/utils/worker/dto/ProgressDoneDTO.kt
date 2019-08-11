package eu.depau.etchdroid.utils.worker.dto

import android.os.Parcel
import android.os.Parcelable
import eu.depau.etchdroid.utils.worker.enums.ErrorType
import eu.depau.kotlet.android.parcelable.*

data class ProgressDoneDTO(
        val jobId: Long,
        val error: ErrorType?,
        val errorData: Parcelable?
) : KotletParcelable {
    constructor(parcel: Parcel) : this(
            jobId = parcel.readLong(),
            error = parcel.readNullable { parcel.readEnum<ErrorType>() },
            errorData = parcel.readNullable { parcel.readParcelable(Parcelable::class.java.classLoader) as Parcelable? }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeLong(jobId)
            writeNullable(error) { writeEnum(it) }
            writeNullable(errorData) { writeParcelable(errorData, flags) }
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::ProgressDoneDTO)
    }
}