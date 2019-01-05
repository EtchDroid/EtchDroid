package eu.depau.etchdroid.worker

import eu.depau.etchdroid.worker.dto.ProgressDoneDTO
import eu.depau.etchdroid.worker.dto.ProgressStartDTO
import eu.depau.etchdroid.worker.dto.ProgressUpdateDTO

interface IProgressListener {
    fun notifyStart(dto: ProgressStartDTO)
    fun notifyProgress(dto: ProgressUpdateDTO)
    fun notifyDone(dto: ProgressDoneDTO)
}