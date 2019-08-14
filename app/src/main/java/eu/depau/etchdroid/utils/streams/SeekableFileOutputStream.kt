package eu.depau.etchdroid.utils.streams

import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream

open class SeekableFileOutputStream : FileOutputStream, Seekable {
    constructor(name: String) : super(name)
    constructor(name: String, append: Boolean) : super(name, append)
    constructor(file: File) : super(file)
    constructor(file: File, append: Boolean) : super(file, append)
    constructor(fdObj: FileDescriptor) : super(fdObj)

    override fun seek(offset: Long): Long {
        val prevPos = channel.position()
        channel.position(prevPos + offset)
        return channel.position() - prevPos
    }
}