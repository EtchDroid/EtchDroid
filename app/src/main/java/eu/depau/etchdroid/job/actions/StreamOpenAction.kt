package eu.depau.etchdroid.job.actions

import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import eu.depau.etchdroid.job.AbstractJobAction
import eu.depau.etchdroid.job.enums.TargetType
import eu.depau.etchdroid.job.enums.TargetType.*
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.worker.IAsyncWorker
import eu.depau.etchdroid.workers.StreamOpenWorker
import eu.depau.etchdroid.workers.enums.StreamDirection
import eu.depau.kotlet.android.parcelable.*
import java.io.Serializable

class StreamOpenAction : AbstractJobAction {
    private val targetDescriptor: Any
    private val targetType: TargetType
    private val destKey: SharedDataType
    private val streamDirection: StreamDirection

    constructor(
            name: StringResBuilder,
            progressWeight: Double,
            checkpoint: Serializable?,
            destKey: SharedDataType,
            streamDirection: StreamDirection,
            targetDescriptor: Any,
            targetType: TargetType,
            showInGUI: Boolean = false,
            runAlways: Boolean = true
    ) : super(name, progressWeight, checkpoint, showInGUI, runAlways) {
        this.destKey = destKey
        this.streamDirection = streamDirection
        this.targetDescriptor = targetDescriptor
        this.targetType = targetType
    }

    override fun getWorker(sharedData: MutableMap<SharedDataType, Any?>): IAsyncWorker {
        return when (targetType) {
            ANDROID_URI   -> StreamOpenWorker(sharedData, destKey, streamDirection, targetType, targetDescriptor as Uri)
            AUMS_BLOCKDEV -> StreamOpenWorker(sharedData, destKey, streamDirection, targetType, targetDescriptor as UsbDevice)
            FS_FILE       -> StreamOpenWorker(sharedData, destKey, streamDirection, targetType, targetDescriptor as String)
        }
    }

    override fun checkPreconditions(): List<StringResBuilder> {
        TODO("not implemented")
    }


    // Parcelable implementation

    constructor(parcel: Parcel) : super(parcel) {
        parcel.apply {
            destKey = readEnum<SharedDataType>()!!
            streamDirection = readEnum<StreamDirection>()!!
            targetType = readEnum<TargetType>()!!
            targetDescriptor = when (targetType) {
                ANDROID_URI   -> readTypedObjectCompat(Uri.CREATOR)
                AUMS_BLOCKDEV -> readTypedObjectCompat(UsbDevice.CREATOR)
                FS_FILE       -> readString()
            }!!
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.apply {
            writeEnum(destKey)
            writeEnum(streamDirection)
            writeEnum(targetType)
            when (targetType) {
                ANDROID_URI, AUMS_BLOCKDEV -> writeTypedObjectCompat(targetDescriptor as Parcelable, flags)
                FS_FILE                    -> writeString(targetDescriptor as String)
            }
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator { Input2OutputStreamCopyAction::class.java }
    }
}