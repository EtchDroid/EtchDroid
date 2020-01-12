package eu.depau.etchdroid.job.actions

import android.os.Parcel
import eu.depau.etchdroid.job.AbstractJobAction
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.worker.IAsyncWorker
import eu.depau.etchdroid.workers.Input2OutputStreamCopyAsyncWorker
import eu.depau.kotlet.android.parcelable.parcelableCreator
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable

class Input2OutputStreamCopyAction : AbstractJobAction {
    constructor(
            name: StringResBuilder,
            progressWeight: Double,
            checkpoint: Serializable?,
            showInGUI: Boolean = true,
            runAlways: Boolean = false
    ) : super(name, progressWeight, checkpoint, showInGUI, runAlways)

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @JvmField
        val CREATOR = parcelableCreator { Input2OutputStreamCopyAction::class.java }
    }

    override fun getWorker(sharedData: MutableMap<SharedDataType, Any?>): IAsyncWorker {
        val inputStream = sharedData[SharedDataType.INPUT_STREAM] as InputStream
        val outputStream = sharedData[SharedDataType.OUTPUT_STREAM] as OutputStream
        val size = sharedData[SharedDataType.SIZE] as Long

        // TODO use checkpoint data

        return Input2OutputStreamCopyAsyncWorker(
                source = inputStream,
                dest = outputStream,
                seek = 0L /*stub*/,
                size = size
        )
    }

    override fun checkPreconditions(): List<StringResBuilder> {
        TODO("not implemented")
    }

}