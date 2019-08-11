package eu.depau.etchdroid.utils.job.impl

import android.os.Parcel
import eu.depau.etchdroid.utils.job.IJobAction
import eu.depau.etchdroid.utils.job.IJobProcedure
import eu.depau.kotlet.android.parcelable.KotletParcelable
import eu.depau.kotlet.android.parcelable.parcelableCreator
import java.io.Serializable

open class JobProcedure(override val nameResId: Int) : ArrayList<IJobAction>(), IJobProcedure, KotletParcelable, Serializable {
    constructor(parcel: Parcel) : this(parcel.readInt()) {
        parcel.readList(this as List<IJobAction>, IJobAction::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeInt(nameResId)
            writeList(this@JobProcedure as List<IJobAction>)
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::JobProcedure)
    }
}