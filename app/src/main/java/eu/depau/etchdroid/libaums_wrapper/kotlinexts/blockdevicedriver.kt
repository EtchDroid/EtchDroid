package eu.depau.etchdroid.libaums_wrapper.kotlinexts

import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.driver.file.FileBlockDeviceDriver
import me.jahnen.libaums.core.driver.scsi.ScsiBlockDevice
import java.io.RandomAccessFile

private fun ScsiBlockDevice.getSize(): Int {
    // Get the size out using reflection - hopefully upstream will merge it
    return ScsiBlockDevice::class.java
        .getDeclaredField("lastBlockAddress")
        .apply { isAccessible = true }
        .get(this) as Int
}

private fun FileBlockDeviceDriver.getSize(): Int {
    val file = FileBlockDeviceDriver::class.java
        .getDeclaredField("file")
        .apply { isAccessible = true }
        .get(this) as RandomAccessFile

    return (file.length() / blockSize).toInt()
}

val BlockDeviceDriver.size: Int
    get() = when (this) {
        is ScsiBlockDevice -> getSize()
        is FileBlockDeviceDriver -> getSize()
        else -> throw NotImplementedError()
    }