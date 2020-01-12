package eu.depau.etchdroid.utils.streams

import java.io.InputStream

abstract class AbstractSizedInputStream: InputStream() {
    abstract val size: Long
}