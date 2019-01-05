package eu.depau.etchdroid.ui.misc

import java.io.File
import java.io.FileDescriptor
import java.io.RandomAccessFile

class SparseFile : RandomAccessFile {
    constructor(file: File, mode: String) : super(file, mode)
    constructor(path: String, mode: String) : super(path, mode)

    fun lseek(fd: FileDescriptor, offset: Long, whence: Int): Long {
        val libcore = Class.forName("libcore.io.Libcore")
        val os = libcore.getField("os").get(null)
        val lseek = os.javaClass.getMethod(
                "lseek", FileDescriptor::class.java, Long::class.java, Int::class.java
        )
        return lseek.invoke(null, fd, offset, whence) as Long
    }
}