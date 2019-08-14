package eu.depau.etchdroid.utils.streams

interface Seekable {
    fun seek(offset: Long): Long
}