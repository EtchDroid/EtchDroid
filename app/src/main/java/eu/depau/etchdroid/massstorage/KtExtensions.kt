package eu.depau.etchdroid.massstorage

import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.driver.file.FileBlockDeviceDriver
import me.jahnen.libaums.core.driver.scsi.ScsiBlockDevice
import java.io.RandomAccessFile

private fun ScsiBlockDevice.getNumBlocks(): Long {
    // Get the size out using reflection - hopefully upstream will merge it
    return (ScsiBlockDevice::class.java
        .getDeclaredField("lastBlockAddress")
        .apply { isAccessible = true }
        .get(this) as Int).toLong()
}

private fun FileBlockDeviceDriver.getNumBlocks(): Long {
    val file = FileBlockDeviceDriver::class.java
        .getDeclaredField("file")
        .apply { isAccessible = true }
        .get(this) as RandomAccessFile

    return file.length() / blockSize
}

val BlockDeviceDriver.numBlocks: Long
    get() = when (this) {
        is ScsiBlockDevice -> getNumBlocks()
        is FileBlockDeviceDriver -> getNumBlocks()
        else -> throw NotImplementedError()
    }