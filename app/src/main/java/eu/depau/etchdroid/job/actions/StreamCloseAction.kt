package eu.depau.etchdroid.job.actions

import android.os.Parcel
import eu.depau.etchdroid.job.AbstractJobAction
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.worker.IAsyncWorker
import eu.depau.etchdroid.workers.StreamCloseWorker
import eu.depau.kotlet.android.parcelable.parcelableCreator
import eu.depau.kotlet.android.parcelable.readEnum
import eu.depau.kotlet.android.parcelable.writeEnum
import java.io.Serializable

class StreamCloseAction : AbstractJobAction {
    private val destKey: SharedDataType

    override fun getWorker(sharedData: MutableMap<SharedDataType, Any?>): IAsyncWorker =
            StreamCloseWorker(sharedData, destKey)

    override fun checkPreconditions(): List<StringResBuilder> {
        TODO("not implemented")
    }

    constructor(
            name: StringResBuilder,
            progressWeight: Double,
            checkpoint: Serializable?,
            destKey: SharedDataType,
            showInGUI: Boolean = true,
            runAlways: Boolean = false
    ) : super(name, progressWeight, checkpoint, showInGUI, runAlways) {
        this.destKey = destKey
    }

    constructor(parcel: Parcel) : super(parcel) {
        this.destKey = parcel.readEnum<SharedDataType>()!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeEnum(destKey)
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator { StreamCloseAction::class.java }
    }
}