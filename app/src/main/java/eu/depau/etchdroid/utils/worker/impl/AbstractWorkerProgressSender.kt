package eu.depau.etchdroid.utils.worker.impl

import eu.depau.etchdroid.utils.worker.IWorkerProgressListener
import eu.depau.etchdroid.utils.worker.IWorkerProgressSender
import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO


abstract class AbstractWorkerProgressSender : IWorkerProgressSender {
    private val listeners = ArrayList<IWorkerProgressListener>()

    override fun attachWorkerProgressListener(listener: IWorkerProgressListener) =
            listeners.add(listener)

    override fun detachWorkerProgressListener(listener: IWorkerProgressListener) =
            listeners.remove(listener)

    protected fun notifyWorkerProgress(dto: ProgressUpdateDTO) =
            listeners.forEach { it.onWorkerProgress(dto) }
}