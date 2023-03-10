package eu.depau.etchdroid.utils

import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

abstract class AsyncOutputStream : OutputStream() {
    override fun write(b: Int) {
        runBlocking { writeAsync(b) }
    }

    open suspend fun writeAsync(b: Int) {
        write(b)
    }

    override fun write(b: ByteArray) {
        runBlocking { writeAsync(b) }
    }

    open suspend fun writeAsync(b: ByteArray) {
        write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        runBlocking { writeAsync(b, off, len) }
    }

    open suspend fun writeAsync(b: ByteArray, off: Int, len: Int) {
        write(b, off, len)
    }

    override fun close() {
        runBlocking { closeAsync() }
    }

    open suspend fun closeAsync() {
        close()
    }

    override fun flush() {
        runBlocking { flushAsync() }
    }

    open suspend fun flushAsync() {
        flush()
    }
}

abstract class AsyncInputStream : InputStream() {
    override fun read(): Int {
        return runBlocking { readAsync() }
    }

    open suspend fun readAsync(): Int {
        return read()
    }

    override fun read(b: ByteArray): Int {
        return runBlocking { readAsync(b) }
    }

    open suspend fun readAsync(b: ByteArray): Int {
        return read(b)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return runBlocking { readAsync(b, off, len) }
    }

    open suspend fun readAsync(b: ByteArray, off: Int, len: Int): Int {
        return read(b, off, len)
    }

    override fun close() {
        runBlocking { closeAsync() }
    }

    open suspend fun closeAsync() {
        close()
    }

    override fun available(): Int {
        return runBlocking { availableAsync() }
    }

    open suspend fun availableAsync(): Int {
        return available()
    }

    override fun mark(readlimit: Int) {
        runBlocking { markAsync(readlimit) }
    }

    open suspend fun markAsync(readlimit: Int) {
        mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return runBlocking { markSupportedAsync() }
    }

    open suspend fun markSupportedAsync(): Boolean {
        return markSupported()
    }

    override fun reset() {
        runBlocking { resetAsync() }
    }

    open suspend fun resetAsync() {
        reset()
    }

    override fun skip(n: Long): Long {
        return runBlocking { skipAsync(n) }
    }

    open suspend fun skipAsync(n: Long): Long {
        return skip(n)
    }
}