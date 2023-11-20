package eu.depau.etchdroid.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout


class TimedMutex private constructor(private val timeout: Long, private val mutex: Mutex) :
    Mutex by mutex {
    constructor(timeoutMillis: Long = defaultTimeout, locked: Boolean = false) : this(
        timeoutMillis, Mutex(locked)
    )

    override suspend fun lock(owner: Any?) {
        if (timeout > 0)
            withTimeout(timeout) {
                mutex.lock(owner)
            }
        else
            mutex.lock(owner)
    }

    companion object {
        var defaultTimeout: Long = -1L
    }
}
