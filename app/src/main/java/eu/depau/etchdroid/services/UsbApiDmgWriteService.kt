package eu.depau.etchdroid.services

import android.hardware.usb.UsbDevice
import android.net.Uri
import com.google.common.util.concurrent.SimpleTimeLimiter
import com.google.common.util.concurrent.TimeLimiter
import com.google.common.util.concurrent.UncheckedTimeoutException
import eu.depau.etchdroid.kotlinexts.getBinary
import eu.depau.etchdroid.kotlinexts.getFileName
import eu.depau.etchdroid.kotlinexts.getFileSize
import eu.depau.etchdroid.kotlinexts.name
import java.io.BufferedReader
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

val plistRegex = Regex("\\s*partition (\\d+): begin=(\\d+), size=(\\d+), decoded=(\\d+), firstsector=(\\d+), sectorcount=(\\d+), blocksruncount=(\\d+)\\s*")
//val progressRegex = Regex("\\[?(\\d+)]\\s+(\\d+[.,]\\d+)%")

class UsbApiDmgWriteService : UsbApiWriteService("UsbApiDmgWriteService") {
    val sectorSize = 512

    private lateinit var uri: Uri
    private lateinit var process: Process
    private lateinit var errReader: BufferedReader

    private var bytesTotal = 0

    private var readTimeLimiter: TimeLimiter = SimpleTimeLimiter.create(Executors.newCachedThreadPool())

    override fun getSendProgress(usbDevice: UsbDevice, uri: Uri): (Long) -> Unit {
        val imageSize = uri.getFileSize(this)
        return { bytes ->
            //            asyncReadProcessProgress()

            try {
                readTimeLimiter.callWithTimeout({
                    val byteArray = ByteArray(128)
                    process.errorStream.read(byteArray)
                    System.err.write(byteArray)
                }, 100, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
            } catch (e: UncheckedTimeoutException) {
            }

            val perc = if (bytesTotal == 0)
                -1
            else
                (bytes.toDouble() / bytesTotal.toDouble() * 100).toInt()

            updateNotification(usbDevice.name, uri.getFileName(this), bytes, perc)
        }
    }

    override fun getInputStream(uri: Uri): InputStream {
        this.uri = uri
        val pb = ProcessBuilder(getBinary("dmg2img").path, "-v", uri.path, "-")
        pb.environment()["LD_LIBRARY_PATH"] = applicationInfo.nativeLibraryDir
        process = pb.start()
        errReader = process.errorStream.bufferedReader()

        // Read blocksruncount
        var matched = false
        var lastSector = 0
        while (true) {
            val line = errReader.readLine() ?: break
            val match = plistRegex.find(line) ?: if (matched) break else continue
            matched = true

            val (begin, size, decoded, firstsector, sectorcount, blocksruncount) = match.destructured

            val partLastSector = firstsector.toInt() + sectorcount.toInt()
            if (partLastSector > lastSector)
                lastSector = partLastSector
        }

        bytesTotal = lastSector * sectorSize
        return process.inputStream
    }
}