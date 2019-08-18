package eu.depau.etchdroid.utils.job.impl

import android.os.Parcel
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.IJobAction
import eu.depau.etchdroid.utils.job.IJobProcedure
import eu.depau.kotlet.android.parcelable.KotletParcelable
import eu.depau.kotlet.android.parcelable.parcelableCreator
import java.io.Serializable

class JobProcedure(override val name: StringResBuilder) :
        ArrayList<IJobAction>(), IJobProcedure, KotletParcelable, Serializable {

    constructor(parcel: Parcel) : this(
            (parcel.readParcelable(StringResBuilder::class.java.classLoader) as StringResBuilder?)!!
    ) {
        parcel.readList(this as List<IJobAction>, IJobAction::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeParcelable(name, flags)
            writeList(this@JobProcedure as List<IJobAction>)
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::JobProcedure)
    }
}