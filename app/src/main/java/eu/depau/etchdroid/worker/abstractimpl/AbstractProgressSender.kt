package eu.depau.etchdroid.worker.abstractimpl

import eu.depau.etchdroid.worker.IProgressListener
import eu.depau.etchdroid.worker.IProgressSender
import eu.depau.etchdroid.worker.dto.ProgressDoneDTO
import eu.depau.etchdroid.worker.dto.ProgressStartDTO
import eu.depau.etchdroid.worker.dto.ProgressUpdateDTO

abstract class AbstractProgressSender : IProgressSender {
    private val listeners = ArrayList<IProgressListener>()

    override fun attachProgressListener(listener: IProgressListener) =
            listeners.add(listener)

    override fun detachProgressListener(listener: IProgressListener) =
            listeners.remove(listener)

    protected fun notifyStart(dto: ProgressStartDTO) =
            listeners.forEach { it.notifyStart(dto) }

    protected fun notifyProgress(dto: ProgressUpdateDTO) =
            listeners.forEach { it.notifyProgress(dto) }

    protected fun notifyDone(dto: ProgressDoneDTO) =
            listeners.forEach { it.notifyDone(dto) }
}