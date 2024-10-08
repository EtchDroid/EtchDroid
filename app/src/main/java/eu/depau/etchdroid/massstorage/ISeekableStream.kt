package eu.depau.etchdroid.massstorage

import kotlinx.coroutines.runBlocking

interface ISeekableStream {
    fun seek(offset: Long): Long {
        return runBlocking { seekAsync(offset) }
    }

    suspend fun seekAsync(offset: Long): Long {
        return seek(offset)
    }
}