package eu.depau.etchdroid.broadcasts

import android.content.Intent
import eu.depau.etchdroid.broadcasts.dto.JobProgressUpdateBroadcastDTO
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import java.io.Serializable

/**
 * Mock reimplementation that shadows the original in order to return mock intents
 */
object JobProgressUpdateBroadcast {
    const val ACTION = "eu.depau.etchdroid.broadcast.JOB_PROGRESS_UPDATE"

    fun getIntent(dto: JobProgressUpdateBroadcastDTO) = mock(Intent::class.java).apply {
        action = ACTION

        // Simulate intent put/get extra logic
        val extras = mutableMapOf<String, Serializable>()

        Mockito
                .`when`(this.putExtra(any(), any(Serializable::class.java)))
                .thenAnswer {
                    val extra = it.arguments[0] as String
                    val serializable = it.arguments[1] as Serializable
                    extras[extra] = serializable
                    this
                }

        Mockito
                .`when`(this.getSerializableExtra(any()))
                .thenAnswer {
                    val extra = it.arguments[0] as String
                    extras[extra]
                }


        dto.writeToIntent(this)
    }
}