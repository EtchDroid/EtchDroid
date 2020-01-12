package eu.depau.etchdroid.job

import android.os.Parcel
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.IJobAction
import eu.depau.kotlet.android.parcelable.*
import java.io.Serializable

abstract class AbstractJobAction(
        override val name: StringResBuilder,
        override val progressWeight: Double,
        override var checkpoint: Serializable? = null,
        override val showInGUI: Boolean = true,
        override val runAlways: Boolean = true
) : IJobAction, KotletParcelable {

    constructor(parcel: Parcel) : this(
            name = parcel.readTypedObjectCompat(StringResBuilder.CREATOR)!!,
            progressWeight = parcel.readDouble(),
            checkpoint = parcel.readNullable { parcel.readSerializable() },
            showInGUI = parcel.readBool(),
            runAlways = parcel.readBool()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeTypedObjectCompat(name, flags)
            writeDouble(progressWeight)
            writeNullable(checkpoint) { writeSerializable(it) }
            writeBool(showInGUI)
            writeBool(runAlways)
        }
    }
}