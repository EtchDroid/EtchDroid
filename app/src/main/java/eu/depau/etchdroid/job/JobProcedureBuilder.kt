package eu.depau.etchdroid.job

import android.content.Context
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Parcel
import eu.depau.etchdroid.job.enums.JobType
import eu.depau.etchdroid.job.enums.JobType.*
import eu.depau.etchdroid.job.enums.TargetType
import eu.depau.etchdroid.job.enums.TargetType.*
import eu.depau.etchdroid.job.procedures.buildInOutStreamCopyProcedure
import eu.depau.etchdroid.utils.assertDebug
import eu.depau.etchdroid.utils.job.IJobProcedure
import eu.depau.etchdroid.utils.job.IJobProcedureBuilder
import eu.depau.kotlet.android.parcelable.*

class JobProcedureBuilder() : IJobProcedureBuilder, KotletParcelable {
    private lateinit var jobType: JobType

    internal var inputType: TargetType? = null
    internal var input: Any? = null

    internal var outputType: TargetType? = null
    internal var output: Any? = null


    override fun build(context: Context): IJobProcedure {
        assertDebug(::jobType.isInitialized) { "Job type not set, aborting" }

        return when (jobType) {
            INOUT_STREAM_COPY           -> buildInOutStreamCopyProcedure(context)
            DMG_EXTRACT_TO_OUTPUTSTREAM -> TODO()
            WINDOWS_INSTALLER           -> TODO()
            MACOS_INSTALLER             -> TODO()
            UNETBOOTIN                  -> TODO()
        }
    }


    fun setInput(uri: Uri) {
        inputType = ANDROID_URI
        input = uri
    }

    fun setInput(usbDev: UsbDevice) {
        inputType = AUMS_BLOCKDEV
        input = usbDev
    }

    fun setInput(path: String) {
        inputType = FS_FILE
        input = path
    }

    fun setOutput(uri: Uri) {
        outputType = ANDROID_URI
        output = uri
    }

    fun setOutput(usbDev: UsbDevice) {
        outputType = AUMS_BLOCKDEV
        output = usbDev
    }

    fun setOutput(path: String) {
        outputType = FS_FILE
        output = path
    }

    constructor(parcel: Parcel) : this() {
        parcel.apply {
            jobType = readEnum<JobType>()!!
            inputType = readNullable { readEnum<TargetType>() }
            outputType = readNullable { readEnum<TargetType>() }

            input = inputType?.let {
                when (it) {
                    ANDROID_URI   -> readTypedObjectCompat(Uri.CREATOR)
                    AUMS_BLOCKDEV -> readTypedObjectCompat(UsbDevice.CREATOR)
                    FS_FILE       -> readString()
                }!!
            }

            output = outputType?.let {
                when (it) {
                    ANDROID_URI   -> readTypedObjectCompat(Uri.CREATOR)
                    AUMS_BLOCKDEV -> readTypedObjectCompat(UsbDevice.CREATOR)
                    FS_FILE       -> readString()
                }!!
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeEnum(jobType)
            writeNullable(inputType) { writeEnum(it) }
            writeNullable(outputType) { writeEnum(it) }

            inputType?.let {
                when (it) {
                    ANDROID_URI   -> writeTypedObjectCompat(input as Uri, flags)
                    AUMS_BLOCKDEV -> writeTypedObjectCompat(input as UsbDevice, flags)
                    FS_FILE       -> writeString(input as String)
                }
            }

            outputType?.let {
                when (it) {
                    ANDROID_URI   -> writeTypedObjectCompat(output as Uri, flags)
                    AUMS_BLOCKDEV -> writeTypedObjectCompat(output as UsbDevice, flags)
                    FS_FILE       -> writeString(output as String)
                }
            }
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator { JobProcedureBuilder::class.java }
    }
}