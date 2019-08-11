package eu.depau.etchdroid.utils.worker.impl

import eu.depau.etchdroid.utils.worker.IProgressListener
import eu.depau.etchdroid.utils.worker.IProgressSender
import eu.depau.etchdroid.utils.worker.dto.ProgressDoneDTO
import eu.depau.etchdroid.utils.worker.dto.ProgressStartDTO
import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO


abstract class AbstractProgressSender : IProgressSender {
    private val listeners = ArrayList<IProgressListener>()

    override fun attachProgressListener(listener: IProgressListener) =
            listeners.add(listener)

    override fun detachProgressListener(listener: IProgressListener) =
            listeners.remove(listener)

    // TODO: fix
//    protected fun notifyStart(dto: ProgressStartDTO) =
//            listeners.forEach { it.notifyActionStart() }

    protected fun notifyProgress(dto: ProgressUpdateDTO) =
            listeners.forEach { it.notifyActionProgress(dto) }

//    protected fun notifyDone(dto: ProgressDoneDTO) =
//            listeners.forEach { it.notifyActionDone() }
}