package eu.depau.etchdroid.utils.worker.dto

import android.os.Parcel
import eu.depau.etchdroid.utils.worker.enums.JobType
import eu.depau.kotlet.android.parcelable.KotletParcelable
import eu.depau.kotlet.android.parcelable.parcelableCreator
import eu.depau.kotlet.android.parcelable.readEnum
import eu.depau.kotlet.android.parcelable.writeEnum

data class ProgressStartDTO(
        val jobId: Long,
        val jobType: JobType,
        val inputName: String,
        val outputName: String,
        val stepsNamesResIDs: List<Int>
) : KotletParcelable {
    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) : this(
            jobId = parcel.readLong(),
            jobType = parcel.readEnum<JobType>()!!,
            inputName = parcel.readString()!!,
            outputName = parcel.readString()!!,
            stepsNamesResIDs = (parcel.readSerializable() as Array<Int>).asList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeLong(jobId)
            writeEnum(jobType)
            writeString(inputName)
            writeString(outputName)
            writeSerializable(stepsNamesResIDs.toTypedArray())
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::ProgressStartDTO)
    }
}