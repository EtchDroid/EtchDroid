package eu.depau.etchdroid.utils.job.impl

import eu.depau.etchdroid.utils.job.IJobProgressListener
import eu.depau.etchdroid.utils.job.IJobProgressSender

abstract class AbstractJobProgressSender : IJobProgressSender {
    private val listeners = ArrayList<IJobProgressListener>()

    override fun attachJobProgressListener(listener: IJobProgressListener): Boolean =
            listeners.add(listener)

    override fun detachJobProgressListener(listener: IJobProgressListener): Boolean =
            listeners.remove(listener)

    protected fun notifyJobActionStart(actionIndex: Int) =
            listeners.forEach { it.onActionStart(actionIndex) }

    protected fun notifyJobActionDone() =
            listeners.forEach { it.onActionDone() }

    protected fun notifyJobProcedureStart(startActionIndex: Int = 0) =
            listeners.forEach { it.onProcedureStart(startActionIndex) }

    protected fun notifyJobProcedureDone() =
            listeners.forEach { it.onProcedureDone() }

    protected fun notifyJobProcedureError(error: Throwable) =
            listeners.forEach { it.onProcedureError(error) }
}