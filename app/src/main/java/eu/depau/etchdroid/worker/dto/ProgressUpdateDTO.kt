package eu.depau.etchdroid.worker.dto

import android.os.Parcel
import eu.depau.etchdroid.worker.enums.RateUnit
import eu.depau.kotlet.android.parcelable.*

data class ProgressUpdateDTO(
        val jobId: Long,
        val currentStep: Int,
        val jobProgress: Double,
        val stepProgress: Double?,
        val timeRemaining: Long?,
        val currentRate: Double?,
        val rateUnit: RateUnit?
) : KotletParcelable {
    constructor(parcel: Parcel) : this(
            jobId = parcel.readLong(),
            currentStep = parcel.readInt(),
            jobProgress = parcel.readDouble(),
            stepProgress = parcel.readNullable { parcel.readDouble() },
            timeRemaining = parcel.readNullable { parcel.readLong() },
            currentRate = parcel.readNullable { parcel.readDouble() },
            rateUnit = parcel.readNullable { parcel.readEnum<RateUnit>() }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeLong(jobId)
            writeInt(currentStep)
            writeDouble(jobProgress)
            writeNullable(stepProgress) { writeDouble(it) }
            writeNullable(timeRemaining) { writeLong(it) }
            writeNullable(currentRate) { writeDouble(it) }
            writeNullable(rateUnit) { writeEnum(rateUnit) }
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::ProgressUpdateDTO)
    }
}