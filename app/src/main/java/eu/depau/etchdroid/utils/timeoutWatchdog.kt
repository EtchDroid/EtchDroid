package eu.depau.etchdroid.utils

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

interface TimeoutBump {
    fun bump()
}

suspend fun <T> timeoutWatchdog(
    timeMillis: Long,
    block: suspend CoroutineScope.(TimeoutBump) -> T,
): T = coroutineScope {
    val expirationTime = AtomicLong(System.currentTimeMillis() + timeMillis)
    val bump = object : TimeoutBump {
        override fun bump() {
            expirationTime.set(System.currentTimeMillis() + timeMillis)
        }
    }

    val result = async {
        block(bump)
    }

    val cancelJob = launch {
        while (true) {
            val delayTime = expirationTime.get() - System.currentTimeMillis()
            if (delayTime <= 0) {
                result.cancel(CancellationException("Timeout expired"))
                break
            } else {
                // Wait for the remaining time
                delay(delayTime)
            }
        }
    }

    result
        .apply { invokeOnCompletion { cancelJob.cancel() } }
        .await()
}
