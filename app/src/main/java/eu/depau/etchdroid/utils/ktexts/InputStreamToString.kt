package eu.depau.etchdroid.utils.ktexts

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@Throws(IOException::class)
fun InputStream.readString(): String {
    val baos = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length = this.read(buffer)

    while (length != -1) {
        baos.write(buffer, 0, length)
        length = this.read(buffer)
    }
    return baos.toString("UTF-8")
}