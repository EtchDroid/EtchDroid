package eu.depau.etchdroid.broadcasts.dto

import android.content.Intent
import eu.depau.etchdroid.utils.worker.enums.RateUnit
import java.io.Serializable


data class JobProgressUpdateBroadcastDTO(
        val jobId: Long,
        val indefinite: Boolean = false,
        val showProgressBar: Boolean = true,
        val completed: Boolean = false,
        val step: Int? = null,
        val percentage: Double? = null,
        val rate: Double? = null,
        val rateUnit: RateUnit? = null,
        val currentMessage: String? = null,
        val error: Throwable? = null
) : Serializable {
    fun writeToIntent(intent: Intent) {
        intent.putExtra(EXTRA, this)
    }

    companion object {
        const val EXTRA = "eu.depau.etchdroid.broadcast.extras.UPDATE_DTO"
        
        fun readFromIntent(intent: Intent) =
                intent.getSerializableExtra(EXTRA)!! as JobProgressUpdateBroadcastDTO
    }
}