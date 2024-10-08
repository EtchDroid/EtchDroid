package eu.depau.etchdroid.massstorage

import kotlinx.coroutines.runBlocking

/**
 * Interface for a seekable stream.
 */
interface ISeekableStream {
    /**
     * Seeks to the given offset.
     * @param offset The offset to seek to.
     * @return The skipped distance.
     */
    fun seek(offset: Long): Long {
        return runBlocking { seekAsync(offset) }
    }

    /**
     * Seeks to the given offset asynchronously.
     * @param offset The offset to seek to.
     * @return The skipped distance.
     */
    suspend fun seekAsync(offset: Long): Long {
        return seek(offset)
    }
}