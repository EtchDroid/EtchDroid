package eu.depau.etchdroid.utils.worker.impl

import eu.depau.etchdroid.utils.worker.IProgressSender
import eu.depau.etchdroid.utils.worker.IWorkerProgressListener
import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO


abstract class AbstractProgressSender : IProgressSender {
    private val listeners = ArrayList<IWorkerProgressListener>()

    override fun attachProgressListener(listener: IWorkerProgressListener) =
            listeners.add(listener)

    override fun detachProgressListener(listener: IWorkerProgressListener) =
            listeners.remove(listener)

    protected fun notifyProgress(dto: ProgressUpdateDTO) =
            listeners.forEach { it.onWorkerProgress(dto) }
}