package eu.depau.etchdroid.worker.dto

import android.os.Parcelable
import eu.depau.etchdroid.worker.enums.ErrorType

data class ProgressDoneDTO(
        val operationId: Int,
        val error: ErrorType?,
        val errorData: Parcelable?
)