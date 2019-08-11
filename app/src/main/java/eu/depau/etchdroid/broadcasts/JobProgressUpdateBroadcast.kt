package eu.depau.etchdroid.broadcasts

import android.content.Intent
import eu.depau.etchdroid.broadcasts.dto.JobProgressUpdateBroadcastDTO

object JobProgressUpdateBroadcast {
    const val ACTION = "eu.depau.etchdroid.broadcast.JOB_PROGRESS_UPDATE"

    fun getIntent(dto: JobProgressUpdateBroadcastDTO) = Intent().apply {
        action = this@JobProgressUpdateBroadcast.ACTION
        dto.writeToIntent(this)
    }
}