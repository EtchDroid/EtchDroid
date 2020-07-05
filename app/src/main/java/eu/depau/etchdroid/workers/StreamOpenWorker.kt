package eu.depau.etchdroid.workers

import android.content.Context
import android.hardware.usb.UsbDevice
import android.net.Uri
import eu.depau.etchdroid.job.enums.TargetType
import eu.depau.etchdroid.job.enums.TargetType.*
import eu.depau.etchdroid.libaums_wrapper.EtchDroidUsbMassStorageDevice
import eu.depau.etchdroid.libaums_wrapper.EtchDroidUsbMassStorageDevice.Companion.getMassStorageDevices
import eu.depau.etchdroid.libaums_wrapper.streams.BlockDeviceInputStream
import eu.depau.etchdroid.libaums_wrapper.streams.BlockDeviceOutputStream
import eu.depau.etchdroid.libaums_wrapper.streams.SeekableFileOutputStream
import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.etchdroid.utils.worker.impl.AbstractAutoProgressAsyncWorker
import eu.depau.etchdroid.workers.enums.StreamDirection
import eu.depau.etchdroid.workers.enums.StreamDirection.INPUT
import eu.depau.etchdroid.workers.enums.StreamDirection.OUTPUT
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class StreamOpenWorker(
        private val sharedData: MutableMap<SharedDataType, Any?>,
        private val destKey: SharedDataType,
        private val streamDirection: StreamDirection,
        private val targetType: TargetType,
        private val targetDescriptor: Any
// Progress is actually never sent so I'm using bogus values here because it still needs to
// implement IWorkerProgressSender
) : AbstractAutoProgressAsyncWorker(0, 0, RateUnit.BYTES_PER_SECOND) {

    private fun getFilesystemStream(): Any {
        val file = File(targetDescriptor as String)

        return when (streamDirection) {
            INPUT  -> FileInputStream(file)
            OUTPUT -> FileOutputStream(file)
        }
    }

    private fun getAndroidUriStream(): Any {
        val context = sharedData[SharedDataType.CONTEXT] as Context
        val uri = targetDescriptor as Uri

        return if (streamDirection == INPUT) {
            context.contentResolver.openInputStream(uri)!!
        } else {
            val fileDescriptor = context.contentResolver
                    .openFileDescriptor(uri, "r")!!.fileDescriptor
            SeekableFileOutputStream(fileDescriptor)
        }
    }

    private fun getAUMSDevice(usbDevice: UsbDevice): EtchDroidUsbMassStorageDevice {
        val context = sharedData[SharedDataType.CONTEXT] as Context
        return usbDevice.getMassStorageDevices(context)[0]
    }

    private fun getAUMSBlockDevStream(): Any {
        val usbDev = (targetDescriptor as UsbDevice)
        val aumsDev = getAUMSDevice(usbDev).apply { init() }
        val blockDev = aumsDev.blockDevices.getValue(0)

        return when (streamDirection) {
            INPUT  -> BlockDeviceInputStream(blockDev)
            OUTPUT -> BlockDeviceOutputStream(blockDev)
        }
    }

    override fun runStep(): Boolean {
        val stream = when (targetType) {
            ANDROID_URI   -> getAndroidUriStream()
            AUMS_BLOCKDEV -> getAUMSBlockDevStream()
            FS_FILE       -> getFilesystemStream()
        }
        sharedData[destKey] = stream
        return false
    }
}