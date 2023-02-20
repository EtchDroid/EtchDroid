package eu.depau.etchdroid.libaums_wrapper.streams

interface Seekable {
    fun seek(offset: Long): Long
}