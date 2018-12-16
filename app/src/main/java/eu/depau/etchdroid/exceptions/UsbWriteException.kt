package eu.depau.etchdroid.exceptions

import eu.depau.etchdroid.kotlinexts.toHRSize
import java.io.IOException

class UsbWriteException(offset: Long, writtenBytes: Long, exc: Exception) : IOException(
        "Write failed at block $offset, ${writtenBytes.toHRSize()} written. Error: $exc", exc
)