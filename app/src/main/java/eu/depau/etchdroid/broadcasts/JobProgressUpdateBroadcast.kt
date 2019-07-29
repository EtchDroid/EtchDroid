package eu.depau.etchdroid.broadcasts

import android.content.Intent
import eu.depau.etchdroid.worker.dto.ProgressUpdateDTO

object JobProgressUpdateBroadcast {
    val action = "eu.depau.etchdroid.broadcast.JOB_PROGRESS_UPDATE"

    fun getIntent(dto: ProgressUpdateDTO) = Intent().apply {
        action = this@JobProgressUpdateBroadcast.action
        putExtra("data", dto)
    }
}