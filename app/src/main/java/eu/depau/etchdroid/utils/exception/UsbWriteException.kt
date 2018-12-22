package eu.depau.etchdroid.utils.exception

import eu.depau.etchdroid.utils.ktexts.toHRSize
import java.io.IOException

class UsbWriteException(offset: Long, writtenBytes: Long, exc: Exception) : IOException(
        "Write failed at block $offset, ${writtenBytes.toHRSize()} written. Error: $exc", exc
)