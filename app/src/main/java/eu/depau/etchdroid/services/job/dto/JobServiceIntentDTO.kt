package eu.depau.etchdroid.services.job.dto

import android.os.Parcel
import android.os.Parcelable
import eu.depau.kotlet.android.parcelable.KotletParcelable
import java.io.Serializable

data class JobServiceIntentDTO(
        val jobId: Long
) : KotletParcelable, Serializable {

    constructor(parcel: Parcel) : this(
            parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeLong(jobId)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<JobServiceIntentDTO> {
        const val EXTRA = "eu.depau.etchdroid.broadcast.extras.JOB_SERVICE_DTO"
        override fun createFromParcel(parcel: Parcel): JobServiceIntentDTO {
            return JobServiceIntentDTO(parcel)
        }

        override fun newArray(size: Int): Array<JobServiceIntentDTO?> {
            return arrayOfNulls(size)
        }
    }
}