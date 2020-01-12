package eu.depau.etchdroid.utils.streams

import java.io.OutputStream

abstract class AbstractSizedOutputStream: OutputStream() {
    abstract val size: Long
}