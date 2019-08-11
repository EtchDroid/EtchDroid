package eu.depau.etchdroid.utils.worker

interface IProgressSender {
    fun attachProgressListener(listener: IWorkerProgressListener): Boolean
    fun detachProgressListener(listener: IWorkerProgressListener): Boolean
}