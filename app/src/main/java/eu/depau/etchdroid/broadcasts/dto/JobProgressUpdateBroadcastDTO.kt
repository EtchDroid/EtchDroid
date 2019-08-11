package eu.depau.etchdroid.broadcasts.dto

import android.content.Intent
import eu.depau.etchdroid.broadcasts.BroadcastExtras
import java.io.Serializable


data class JobProgressUpdateBroadcastDTO(
        val jobId: Long,
        val indefinite: Boolean,
        val showProgressBar: Boolean,
        val completed: Boolean,
        val step: Int?,
        val percentage: Double?,
        val currentMessage: String?,
        val error: Exception?
) : Serializable {
    fun writeToIntent(intent: Intent) {
        intent.putExtra(BroadcastExtras.UPDATE_DTO, this)
    }

    companion object {
        fun readFromIntent(intent: Intent) =
                intent.getSerializableExtra(BroadcastExtras.UPDATE_DTO)!! as JobProgressUpdateBroadcastDTO
    }
}